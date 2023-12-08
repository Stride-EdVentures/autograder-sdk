package sdk;
import assignments.AutograderAssignment;
import authentication.AuthenticationRequest;
import authentication.AuthenticationResponse;
import authentication.InviteTeacherRequest;
import authentication.User;
import classes.AutograderClass;
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
import profiles.ProfileResponse;
import storage.FileRequest;
import storage.SortOptions;
import storage.SubmissionResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A client that interacts with the Autograder servers. Internally, this accesses the
 * REST API for the database (POSTGREST) and the storage buckets (AWS S3) hosted by
 * Supabase. As such, the client requires access to two public values: the base URL
 * of the Supabase instance and the anonymous key. The anonymous key can be safely
 * kept inside of the application's code as minimum RLS policies have been added
 * to all resources.
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

    public AutograderClient(String supabaseBaseUrl, String supabaseAnonKey) {
        this.supabaseBaseUrl = supabaseBaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
        this.accessToken = supabaseAnonKey;
        this.authenticatedUser = null;
    }

    /**
     * Authenticates the user and stores the access token for future calls. Other methods
     * can be accessed without calling this method first, however, this approach restricts
     * access to public objects. This method allows the client to gain access to resources
     * that the owner of the account is able to access through RLS policies.
     *
     * @param email The email of the account to authenticate as.
     * @param password The password of the account to authenticate as.
     * @return The successful response from the server with the details of the authenticated user.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public AuthenticationResponse authenticateUser(String email, String password) throws IOException {
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        AuthenticationUrl url = new AuthenticationUrl(this.supabaseBaseUrl + "/auth/v1/token");
        url.grant_type = "password";

        HttpRequest request = requestFactory.buildPostRequest(
                url,
                new JsonHttpContent(JSON_FACTORY, new AuthenticationRequest(email, password))
        );

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
     * Gets the profile of the user with the userId provided. The profile contains some
     * additional information about the user, mainly pertaining to Autograder itself.
     * @param userId The id of the user whose profile must be retrieved.
     * @return The profile of associated with the user, along with the classes and assignments
     *         associated with the user.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public ProfileResponse getUserProfile(String userId) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        String queryString = RestQueryBuilder.from("profiles")
                .select("*,classes(*, assignments(*))")
                .equals("id", userId)
                .generateQuery();

        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                this.supabaseBaseUrl + queryString
        ));

        HttpHeaders headers = request.getHeaders();
        headers.set("apikey", this.supabaseAnonKey);
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        if (httpResponse.isSuccessStatusCode()) {
            ProfileResponse[] profileResponses = httpResponse.parseAs(ProfileResponse[].class);
            if (profileResponses.length > 0) {
                return profileResponses[0];
            }
        }

        return null;
    }

    /**
     * Invites a teacher to the Autograder application by emailing them.
     * @param email The email of the teacher to invite.
     * @return Whether the request was successful.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
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
                new JsonHttpContent(JSON_FACTORY, new InviteTeacherRequest(this.authenticatedUser.email, email))
        );

        HttpHeaders headers = request.getHeaders();
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        return httpResponse.parseAs(Boolean.TYPE);
    }

    /**
     * Gets a list of all the students in a class. This function is almost identical to the getUserProfilesInClass
     * method, except that it does not include the profiles of any teachers.
     * @param classId The id of the class to use when retrieving associated profiles.
     * @return A list of all the user profiles or null if an error occurred.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public List<ProfileResponse> getStudentsInClass(String classId) throws IOException {
        return getUserProfilesInClass(classId, false);
    }

    /**
     * Gets a list of all the users in a class, including both students and teachers.
     * @param classId The id of the class to use when retrieving associated profiles.
     * @return A list of all the user profiles or null if an error occurred.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public List<ProfileResponse> getUserProfilesInClass(String classId, boolean includeTeachers) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        RestQueryBuilder queryBuilder = RestQueryBuilder.from("profiles")
                .select("*,classes!inner(*, assignments(*))")
                .equals("classes.id", classId);

        if (!includeTeachers) {
            queryBuilder = queryBuilder.notEquals("is_teacher", true);
        }

        String queryString = queryBuilder.generateQuery();

        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                this.supabaseBaseUrl + queryString
        ));

        HttpHeaders headers = request.getHeaders();
        headers.set("apikey", this.supabaseAnonKey);
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        if (httpResponse.isSuccessStatusCode()) {
            ProfileResponse[] profileResponses = httpResponse.parseAs(ProfileResponse[].class);
            if (profileResponses.length > 0) {
                return new ArrayList<>(Arrays.asList(profileResponses));
            }
        }

        return null;
    }

    /**
     * Gets a list of all students that have submitted a particular assignment, where the submitted refers to a student
     * submitting all required files.
     * @param classId The id of the class which contains the students interested.
     * @param assignmentId The id of the assignment.
     * @return The list of students that fully submitted the assignment.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public List<ProfileResponse> getSubmittedStudents(String classId, String assignmentId) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        List<ProfileResponse> profilesInClass = getStudentsInClass(classId);
        if (profilesInClass == null || profilesInClass.size() == 0) {
            return null;
        }

        Optional<AutograderClass> selectedClass = Arrays.stream(profilesInClass.get(0).classes)
                .filter(classInformation -> classInformation.id.equals(classId))
                .findFirst();

        if (!selectedClass.isPresent()) {
            return null;
        }

        Optional<AutograderAssignment> selectedAssignment = Arrays.stream(selectedClass.get().assignments)
                .filter(assignment -> assignment.id.equals(assignmentId))
                .findFirst();

        if (!selectedAssignment.isPresent()) {
            return null;
        }

        List<ProfileResponse> submittedProfiles = new ArrayList<>();
        for (ProfileResponse profile : profilesInClass) {
            String latestVersion = getLatestSubmittedVersion(profile.id, assignmentId);
            List<SubmissionResponse> submittedFiles = getSubmittedFilesForVersion(profile.id, assignmentId, latestVersion);
            if (isCompleteSubmission(submittedFiles, selectedAssignment.get())) {
                submittedProfiles.add(profile);
            }
        }

        return submittedProfiles;
    }

    /**
     * Gets a list of all the versions submitted for a particular assignment by a student. Internally, this is represented
     * as the folders that are underneath the student and the assignment, in terms of directory structure.
     * @param studentId The id of the student.
     * @param assignmentId The id of the assignment for which the student submitted files for.
     * @return The list of versions submitted by the student for this assignment.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public List<SubmissionResponse> getSubmittedVersionsForAssignment(String studentId, String assignmentId) throws IOException {
        return getFilesInformation("submissions", studentId + "/" + assignmentId);
    }

    /**
     * Gets the latest version submitted by the student for the assignment with the given id. This is a helper method
     * which internally gets all the submitted versions and performs the filter internally.
     * @param studentId The id of the student.
     * @param assignmentId The id of the assignment for which the student submitted files for.
     * @return The highest version submitted by the student for this assignment.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public String getLatestSubmittedVersion(String studentId, String assignmentId) throws IOException {
        List<SubmissionResponse> submissionResponses = getSubmittedVersionsForAssignment(studentId, assignmentId);
        if (submissionResponses == null) {
            return null;
        }

        OptionalInt highestVersion = submissionResponses.stream()
                .map(submission -> submission.name)
                .filter(submission -> submission.startsWith("v"))
                .mapToInt(submission -> Integer.parseInt(submission.substring(1)))
                .max();

        if (highestVersion.isPresent()) {
            return "v" + highestVersion.getAsInt();
        } else {
            return null;
        }
    }

    /**
     * Gets the files that were submitted for a particular submission version by the student for the assignment
     * with the given id.
     * @param studentId The id of the student.
     * @param assignmentId The id of the assignment for which the student submitted files for.
     * @param version The submission version from which to retrieve the files.
     * @return The files submitted under this submission version by this student for this assignment.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public List<SubmissionResponse> getSubmittedFilesForVersion(String studentId, String assignmentId, String version) throws IOException {
        return getFilesInformation("submissions", studentId + "/" + assignmentId + "/" + version);
    }
    
    /**
     * Get the contents of the file with the given file name. This method returns the contents in plain text as opposed
     * to the bytes constituting the file.
     * @param studentId The id of student.
     * @param assignmentId The id of the assignment that the student submitted files for.
     * @param version The submission version to use.
     * @param fileName The name of the file to download.
     * @return The InputStream of the file downloaded.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public InputStream getFileInputStream(String studentId, String assignmentId, String version, String fileName) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        String path = "submissions/" + studentId + "/" + assignmentId + "/" + version + "/" + fileName;
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                this.supabaseBaseUrl + "/storage/v1/object/" + path
        ));

        HttpHeaders headers = request.getHeaders();
        headers.set("apikey", this.supabaseAnonKey);
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        if (httpResponse.isSuccessStatusCode()) {
            return httpResponse.getContent();
        }

        return null;
    }

    /**
     * Get the contents of the file with the given file name. This method returns the contents in plain text as opposed
     * to the bytes constituting the file.
     * @param studentId The id of student.
     * @param assignmentId The id of the assignment that the student submitted files for.
     * @param version The submission version to use.
     * @param fileName The name of the file to download.
     * @return The contents of the file downloaded.
     * @throws IOException If the request could not be successfully sent, an IOException is thrown.
     */
    public String downloadFile(String studentId, String assignmentId, String version, String fileName) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        String path = "submissions/" + studentId + "/" + assignmentId + "/" + version + "/" + fileName;
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                this.supabaseBaseUrl + "/storage/v1/object/" + path
        ));

        HttpHeaders headers = request.getHeaders();
        headers.set("apikey", this.supabaseAnonKey);
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        if (httpResponse.isSuccessStatusCode()) {
            return httpResponse.parseAsString();
        }

        return null;
    }

    private List<SubmissionResponse> getFilesInformation(String bucketName, String path) throws IOException {
        if (this.accessToken == null) {
            return null;
        }

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> {
            request.setParser(new JsonObjectParser(JSON_FACTORY));
        });

        FileRequest fileRequest = new FileRequest();
        fileRequest.limit = 100;
        fileRequest.offset = 0;
        SortOptions sortOptions = new SortOptions();
        sortOptions.column = "name";
        sortOptions.order = "asc";
        fileRequest.sortBy = sortOptions;
        fileRequest.prefix = path;

        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(
                this.supabaseBaseUrl + "/storage/v1/object/list/" + bucketName
        ), new JsonHttpContent(JSON_FACTORY, fileRequest));

        HttpHeaders headers = request.getHeaders();
        headers.set("apikey", this.supabaseAnonKey);
        headers.setAuthorization("Bearer " + this.accessToken);
        HttpResponse httpResponse = request.execute();
        if (httpResponse.isSuccessStatusCode()) {
            SubmissionResponse[] submissionResponses = httpResponse.parseAs(SubmissionResponse[].class);
            List<SubmissionResponse> filteredSubmissionResponses = Arrays.stream(submissionResponses)
                    .filter(submission -> !submission.name.equals(".emptyFolderPlaceholder"))
                    .collect(Collectors.toList());

            if (filteredSubmissionResponses.size() > 0) {
                return filteredSubmissionResponses;
            }
        }

        return null;
    }

    private boolean isCompleteSubmission(List<SubmissionResponse> submittedFiles, AutograderAssignment assignment) {
        Set<String> submittedFileNames = submittedFiles.stream()
                .map(submission -> submission.name)
                .collect(Collectors.toSet());

        for (String requiredFileName : assignment.required_files) {
            if (!submittedFileNames.contains(requiredFileName)) {
                return false;
            }
        }

        return true;
    }

    public String serialize(Object object, boolean shouldPrettyPrint) throws IOException {
        if (shouldPrettyPrint) {
            return JSON_FACTORY.toPrettyString(object);
        } else {
            return JSON_FACTORY.toString(object);
        }
    }
}