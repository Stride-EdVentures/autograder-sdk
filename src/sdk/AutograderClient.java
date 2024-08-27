package sdk;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import assignments.AutograderAssignment;
import authentication.AuthenticationRequest;
import authentication.AuthenticationResponse;
import authentication.InviteTeacherRequest;
import authentication.User;
import classes.AutograderClass;
import enrollments.EnrollmentResponse;
import profiles.ProfileResponse;
import storage.FileRequest;
import storage.SortOptions;
import storage.SubmissionResponse;
import submissions.AssignmentSubmissionResponse;

/**
 * A client that interacts with the Autograder servers. Internally, this
 * accesses the REST API for the database (POSTGREST) and the storage buckets
 * (AWS S3) hosted by Supabase. As such, the client requires access to two
 * public values: the base URL of the Supabase instance and the anonymous key.
 * The anonymous key can be safely kept inside of the application's code as
 * minimum RLS policies have been added to all resources.
 *
 * @author Revanth Pothukuchi (revanthpothukuchi123@gmail.com)
 * @version 1.0.0
 */
public class AutograderClient {
	private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private final static JsonFactory JSON_FACTORY = new JacksonFactory();
	private final String supabaseBaseUrl;
	private final String supabaseAnonKey;
	private final String AUTOGRADER_BASE_URL = "https://autograder-nchs.vercel.app";
	private String accessToken;
	private User authenticatedUser;
	private HttpRequestFactory requestFactory;

	public AutograderClient(String supabaseBaseUrl, String supabaseAnonKey) {
		this.supabaseBaseUrl = supabaseBaseUrl;
		this.supabaseAnonKey = supabaseAnonKey;
		this.accessToken = supabaseAnonKey;
		this.authenticatedUser = null;
		this.requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
			request.setParser(new JsonObjectParser(JSON_FACTORY));
		});
	}

	/**
	 * Authenticates the user and stores the access token for future calls. Other
	 * methods can be accessed without calling this method first, however, this
	 * approach restricts access to public objects. This method allows the client to
	 * gain access to resources that the owner of the account is able to access
	 * through RLS policies.
	 *
	 * @param email    The email of the account to authenticate as.
	 * @param password The password of the account to authenticate as.
	 * @return The successful response from the server with the details of the
	 *         authenticated user.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 */
	public AuthenticationResponse authenticateUser(String email, String password) throws IOException {
		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
			request.setParser(new JsonObjectParser(JSON_FACTORY));
		});

		AuthenticationUrl url = new AuthenticationUrl(this.supabaseBaseUrl + "/auth/v1/token");
		url.grant_type = "password";

		HttpRequest request = requestFactory.buildPostRequest(url,
				new JsonHttpContent(JSON_FACTORY, new AuthenticationRequest(email, password)));

		HttpHeaders headers = request.getHeaders();
		headers.set("apikey", this.supabaseAnonKey);
		HttpResponse httpResponse = request.execute();
		if (httpResponse.isSuccessStatusCode()) {
			AuthenticationResponse authenticationResponse = httpResponse.parseAs(AuthenticationResponse.class);
			this.accessToken = authenticationResponse.access_token;
			this.authenticatedUser = authenticationResponse.user;
			return authenticationResponse;
		}

		return null;
	}

	/**
	 * Gets the profile of the user with the userId provided. The profile contains
	 * some additional information about the user, mainly pertaining to Autograder
	 * itself.
	 * 
	 * @param userId The id of the user whose profile must be retrieved.
	 * @return The profile of associated with the user, along with the classes and
	 *         assignments associated with the user.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public ProfileResponse getUserProfile(String userId) throws IOException {
		if (this.accessToken == null) {
			return null;
		}

		String queryString = RestQueryBuilder.from("enrollment")//
				.select("*,class(*, assignment(*)),profile(*))") //
				.equals("profile_id", userId) //
				.generateQuery();

		HttpResponse httpResponse = this.createGetRequest(queryString).execute();
		if (httpResponse.isSuccessStatusCode()) {
			EnrollmentResponse[] enrollments = httpResponse.parseAs(EnrollmentResponse[].class);
			if (enrollments.length > 0) {
				return this.enrollments2profiles(enrollments).getFirst();
			}
		}

		return null;
	}

	/**
	 * Invites a teacher to the Autograder application by emailing them.
	 * 
	 * @param email The email of the teacher to invite.
	 * @return Whether the request was successful.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 */
	public boolean inviteTeacher(String email) throws IOException {
		if (this.accessToken == null) {
			return false;
		}

		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
			request.setParser(new JsonObjectParser(JSON_FACTORY));
		});

		HttpRequest request = requestFactory.buildPostRequest(
				new GenericUrl(this.AUTOGRADER_BASE_URL + "/api/auth/inviteTeacher"),
				new JsonHttpContent(JSON_FACTORY, new InviteTeacherRequest(this.authenticatedUser.email, email)));

		HttpHeaders headers = request.getHeaders();
		headers.setAuthorization("Bearer " + this.accessToken);
		HttpResponse httpResponse = request.execute();
		return httpResponse.parseAs(Boolean.TYPE);
	}

	/**
	 * Gets a list of all the students in a class. This function is almost identical
	 * to the getUserProfilesInClass method, except that it does not include the
	 * profiles of any teachers.
	 * 
	 * @param classId The id of the class to use when retrieving associated
	 *                profiles.
	 * @return A list of all the user profiles or null if an error occurred.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public List<ProfileResponse> getStudentsInClass(String classId) throws IOException {
		return getUserProfilesInClass(classId, true);
	}

	/**
	 * Gets a list of all the users in a class, including both students and
	 * teachers.
	 * 
	 * @param classId The id of the class to use when retrieving associated
	 *                profiles.
	 * @return A list of all the user profiles or null if an error occurred.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 *
	 *                     TESTED
	 */
	public List<ProfileResponse> getUserProfilesInClass(String classId, boolean studentsOnly) throws IOException {
		if (this.accessToken != null) {
			RestQueryBuilder queryBuilder = RestQueryBuilder.from("enrollment") //
					.select("*, profile(*), class(*, assignment(*))") //
					.equals("class_id", classId);

			if (studentsOnly) {
				queryBuilder = queryBuilder.equals("type", "student");
			}

			String queryString = queryBuilder.generateQuery();

			HttpRequest request = this.createGetRequest(queryString);

			HttpResponse httpResponse = request.execute();
			if (httpResponse.isSuccessStatusCode()) {
				EnrollmentResponse[] enrollments = httpResponse.parseAs(EnrollmentResponse[].class);
				return this.enrollments2profiles(enrollments);
			}
		}
		return new ArrayList<>();
	}

	/**
	 * @param profileId
	 * @param assignmentId
	 * @return
	 * @throws IOException
	 * 
	 *                     TESTED
	 */
	public List<AssignmentSubmissionResponse> getAssignmentSubmissions(String profileId, String assignmentId)
			throws IOException {
		if (this.accessToken == null) {
			return null;
		}

		RestQueryBuilder queryBuilder = RestQueryBuilder.from("submission") //
				.select("*") //
				.equals("assignment_id", assignmentId) //
				.equals("profile_id", profileId);
		String queryString = queryBuilder.generateQuery();

		HttpResponse httpResponse = this.createGetRequest(queryString).execute();
		if (httpResponse.isSuccessStatusCode()) {
			AssignmentSubmissionResponse[] submissions = httpResponse.parseAs(AssignmentSubmissionResponse[].class);
			return Arrays.asList(submissions);
		}

		return null;
	}

	/**
	 * @param profileId
	 * @param assignmentId
	 * @param version
	 * @param fileName
	 * @return
	 * @throws IOException
	 * 
	 *                     TESTED
	 */
	public AssignmentSubmissionResponse getAssignmentSubmission(String profileId, String assignmentId, Integer version,
			String fileName) throws IOException {
		if (this.accessToken == null) {
			return null;
		}
		RestQueryBuilder queryBuilder = RestQueryBuilder.from("submission").select("*") //
				.equals("assignment_id", assignmentId) //
				.equals("profile_id", profileId) // ;
				.equals("version", version) // ;
				.equals("file_name", fileName);

		String queryString = queryBuilder.generateQuery();

		HttpRequest request = this.createGetRequest(queryString);
		HttpResponse httpResponse = request.execute();
		if (httpResponse.isSuccessStatusCode()) {
			AssignmentSubmissionResponse[] submissions = httpResponse.parseAs(AssignmentSubmissionResponse[].class);
			return submissions.length > 0 ? submissions[0] : null;
		}

		return null;
	}

	/**
	 * Gets a list of all students that have submitted a particular assignment,
	 * where the submitted refers to a student submitting all required files.
	 * 
	 * @param classId      The id of the class which contains the students
	 *                     interested.
	 * @param assignmentId The id of the assignment.
	 * @return The list of students that fully submitted the assignment.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public List<ProfileResponse> getSubmittedStudents(String classId, String assignmentId) throws IOException {
		if (this.accessToken == null) {
			return null;
		}

		AutograderClass autograderClass = getClass(classId);
		if (autograderClass != null) {
			AutograderAssignment selectedAssignment = Arrays.stream(autograderClass.assignments)
					.filter(assignment -> assignment.id.equals(assignmentId)) //
					.findFirst() //
					.orElseThrow(() -> new RuntimeException(
							String.format("Assignment '%s' not in Class '%s'.", assignmentId, classId)));

			List<ProfileResponse> profilesInClass = this.getStudentsInClass(classId);
			List<ProfileResponse> submittedProfiles = new ArrayList<>();
			for (ProfileResponse profile : profilesInClass) {
				List<AssignmentSubmissionResponse> subs = getAssignmentSubmissions(profile.id, assignmentId);
				if (isCompleteSubmission(subs, selectedAssignment)) {
					submittedProfiles.add(profile);
				}
			}
			return submittedProfiles;
		} else {
			throw new RuntimeException(String.format("Class '%s' does not exist.", classId));
		}
	}

	/**
	 * Gets a list of all the versions submitted for a particular assignment by a
	 * student. Internally, this is represented as the folders that are underneath
	 * the student and the assignment, in terms of directory structure.
	 * 
	 * @param studentId    The id of the student.
	 * @param assignmentId The id of the assignment for which the student submitted
	 *                     files for.
	 * @return The list of versions submitted by the student for this assignment.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public List<SubmissionResponse> getSubmittedVersionsForAssignment(String studentId, String assignmentId)
			throws IOException {
		return this.getAssignmentSubmissions(studentId, assignmentId) //
				.stream() //
				.map(s -> {
					SubmissionResponse submission = new SubmissionResponse();
					submission.id = s.id;
					submission.created_at = s.created_at;
					submission.name = s.fileName;
					submission.version = s.version;
					// not tracked in submission table still needed?
					submission.updated_at = s.created_at;
					submission.updated_at = s.created_at;
					return submission;
				}).collect(Collectors.toList());
	}

	/**
	 * Gets the latest version submitted by the student for the assignment with the
	 * given id. This is a helper method which internally gets all the submitted
	 * versions and performs the filter internally.
	 * 
	 * @param studentId    The id of the student.
	 * @param assignmentId The id of the assignment for which the student submitted
	 *                     files for.
	 * @return The highest version submitted by the student for this assignment.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 *
	 *                     TESTED
	 */
	public String getLatestSubmittedVersion(String studentId, String assignmentId) throws IOException {

		String queryString = RestQueryBuilder.from("submission") //
				.select("*") //
				.equals("assignment_id", assignmentId) //
				.equals("profile_id", studentId).generateQuery();

		HttpResponse httpResponse = this.createGetRequest(queryString).execute();
		if (httpResponse.isSuccessStatusCode()) {
			List<AssignmentSubmissionResponse> submissions = Arrays
					.asList(httpResponse.parseAs(AssignmentSubmissionResponse[].class));
			Integer max = submissions.stream() //
					.mapToInt(s -> s.version) //
					.max() //
					.getAsInt();
			return "v" + max;
		}

		return null;
	}

	/**
	 * Gets the latest version submitted by the student for the assignment with the
	 * given id. This is a helper method which internally gets all the submitted
	 * versions and performs the filter internally.
	 * 
	 * @param studentId    The id of the student.
	 * @param assignmentId The id of the assignment for which the student submitted
	 *                     files for.
	 * @return The highest version submitted by the student for this assignment.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public String getLatestSubmittedVersion(String studentId, String assignmentId, String fileName) throws IOException {

		String queryString = RestQueryBuilder.from("submission") //
				.select("*") //
				.equals("assignment_id", assignmentId) //
				.equals("profile_id", studentId) //
				.equals("file_name", fileName) //
				.generateQuery();

		HttpResponse httpResponse = this.createGetRequest(queryString).execute();
		if (httpResponse.isSuccessStatusCode()) {
			List<AssignmentSubmissionResponse> submissions = Arrays
					.asList(httpResponse.parseAs(AssignmentSubmissionResponse[].class));
			Integer max = submissions.stream() //
					.mapToInt(s -> s.version) //
					.max() //
					.getAsInt();
			return "v" + max;
		}

		return null;
	}

	/**
	 * Get the contents of the file with the given file name. This method returns
	 * the contents in plain text as opposed to the bytes constituting the file.
	 * 
	 * @param studentId    The id of student.
	 * @param assignmentId The id of the assignment that the student submitted files
	 *                     for.
	 * @param version      The submission version to use.
	 * @param fileName     The name of the file to download.
	 * @return The InputStream of the file downloaded.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public InputStream getFileInputStream(String studentId, String assignmentId, String version, String fileName)
			throws IOException {
		if (this.accessToken == null) {
			return null;
		}
		ProfileResponse profile = this.getUserProfile(studentId);

		AssignmentSubmissionResponse submission = this.getAssignmentSubmission(studentId, assignmentId,
				Integer.valueOf(version), fileName);

		if (submission == null) {
			throw new RuntimeException(String.format("File does not exist '%s' does not exist.", fileName));
		}

		String path = "/storage/v1/object/submissions/" + profile.authId + "/" + submission.id;

		HttpResponse httpResponse = this.createGetRequest(path).execute();
		if (httpResponse.isSuccessStatusCode()) {
			return httpResponse.getContent();
		}

		return null;
	}

	/**
	 * Get the contents of the file with the given file name. This method returns
	 * the contents in plain text as opposed to the bytes constituting the file.
	 * 
	 * @param studentId    The id of student.
	 * @param assignmentId The id of the assignment that the student submitted files
	 *                     for.
	 * @param version      The submission version to use.
	 * @param fileName     The name of the file to download.
	 * @return The contents of the file downloaded.
	 * @throws IOException If the request could not be successfully sent, an
	 *                     IOException is thrown.
	 * 
	 *                     TESTED
	 */
	public String downloadFile(String profileId, String assignmentId, String version, String fileName)
			throws IOException {
		if (this.accessToken == null) {
			return null;
		}

		ProfileResponse profile = this.getUserProfile(profileId);

		AssignmentSubmissionResponse submission = this.getAssignmentSubmission(profileId, assignmentId,
				Integer.valueOf(version), fileName);

		if (submission == null) {
			throw new RuntimeException(String.format("File does not exist '%s' does not exist.", fileName));
		}
		String path = "/storage/v1/object/submissions/" + profile.authId + "/" + submission.id;

		HttpResponse httpResponse = this.createGetRequest(path).execute();
		if (httpResponse.isSuccessStatusCode()) {
			return httpResponse.parseAsString();
		}

		return null;
	}

	private List<ProfileResponse> enrollments2profiles(EnrollmentResponse[] enrollments) {
		Map<String, ProfileResponse> profilesMap = new HashMap<>();
		for (EnrollmentResponse enrollment : enrollments) {
			ProfileResponse profile = profilesMap.get(enrollment.profile.id);
			if (profile == null) {
				enrollment.profile.classes = new AutograderClass[0];
				profilesMap.put(enrollment.profile.id, enrollment.profile);
				profile = enrollment.profile;
			}
			List<AutograderClass> classes = new ArrayList<>(Arrays.asList(profile.classes));
			classes.add(enrollment.singleClass);
			profile.classes = classes.toArray(new AutograderClass[classes.size()]);
		}
		return new ArrayList<>(profilesMap.values());
	}

	private boolean oldisCompleteSubmission(List<SubmissionResponse> submittedFiles, AutograderAssignment assignment) {
		Set<String> submittedFileNames = submittedFiles //
				.stream() //
				.map(submission -> submission.name) //
				.collect(Collectors.toSet());

		for (String requiredFileName : assignment.required_files) {
			if (!submittedFileNames.contains(requiredFileName)) {
				return false;
			}
		}

		return true;
	}

	private boolean isCompleteSubmission(List<AssignmentSubmissionResponse> submittedFiles,
			AutograderAssignment assignment) {
		Set<String> submittedFileNames = submittedFiles //
				.stream() //
				.map(submission -> submission.fileName) //
				.collect(Collectors.toSet());

		for (String requiredFileName : assignment.required_files) {
			if (!submittedFileNames.contains(requiredFileName)) {
				return false;
			}
		}

		return true;
	}

	private HttpRequest createGetRequest(String queryString) throws IOException {
		HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(this.supabaseBaseUrl + queryString));
		HttpHeaders headers = request.getHeaders();
		headers.set("apikey", this.supabaseAnonKey);
		headers.setAuthorization("Bearer " + this.accessToken);
		return request;
	}

	public AutograderClass getClass(String classId) throws IOException {
		String queryString = RestQueryBuilder.from("class") //
				.select("*, assignment(*)") //
				.equals("id", classId) //
				.generateQuery();

		HttpResponse httpResponse = this.createGetRequest(queryString).execute();
		if (httpResponse.isSuccessStatusCode()) {
			AutograderClass[] clazzes = httpResponse.parseAs(AutograderClass[].class);
			return clazzes.length > 0 ? clazzes[0] : null;
		}
		return null;
	}

	public String serialize(Object object, boolean shouldPrettyPrint) throws IOException {
		if (shouldPrettyPrint) {
			return JSON_FACTORY.toPrettyString(object);
		} else {
			return JSON_FACTORY.toString(object);
		}
	}
}