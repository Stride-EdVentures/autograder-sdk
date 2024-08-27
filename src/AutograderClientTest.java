import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import classes.AutograderClass;
import profiles.ProfileResponse;
import sdk.AutograderClient;
import storage.SubmissionResponse;
import submissions.AssignmentSubmissionResponse;

public class AutograderClientTest {

	// Note: one needs to update the URL and ANON to the correct Service Role values
	AutograderClient client = new AutograderClient(SupabaseSecrets.SUPA_URL, SupabaseSecrets.SUPA_ANON);

	// Class name "class 2" with 1 student (jeffrey.nakasone@digitalaidseattle.org) 
	// and 1 teacher (jnakaso@yahoo.com). No assignments
	public static final String classId = "1090d880-e9ed-4147-95c9-a62578a63429";
	
	// Student (jeffrey.nakasone@digitalaidseattle.org)
	public static final String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
	
	// Class name "CS 101" which has an Assignment
	public static final String classId2 = "47cb01e0-ad85-4225-b46f-4899674a7159";
	
	// In the class "CS 101". 
	// name: "Week One"    description: "Data structures"
	// blacklisted: ["*.exe", "dict.txt"]
	// required: ["test.java"]
	public static final String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
	
	public static String[] passFail = { "PASS", "FAIL" };
	
	public static void main(String[] args) throws IOException {
		
		AutograderClientTest tester = new AutograderClientTest();
		System.out.println("\n **** testing STARTED **** \n");
		tester.testGetStudentsInClass();
		tester.getUserProfilesInClass();
		tester.testGetUserProfile();
		tester.testGetAssignmentSubmissions();
		tester.testGetAssignmentSubmission();
		tester.testDownloadFile();
		tester.testGetLatestSubmittedVersion();
		tester.testGetLatestSubmittedVersionFileName();
		tester.testGetClass();
		tester.testGetSubmittedStudents();
		tester.testGetSubmittedVersionsForAssignment();
		tester.testGetFileInputStream();
		System.out.println("\n **** testing DONE **** ");
	}

	public static void printResults(String actual, String expected) {
	   int pass = actual.equals(expected) ? 0 : 1;
	   System.out.println(passFail[pass]);
	   if (pass == 1) {
	      System.out.printf("\tExpected:%s\n\tActual:%s\n\n", expected, actual);
	   }
	}
	
	public void testGetStudentsInClass() throws IOException {
	   final String expected = "[email:jeffrey.nakasone@digitalaidseattle.org, ID:0de3cf6c-0464-4e7b-9d69-46b2b0162636, Classes:[name:class 2, quarter:Spring 2024, assignments[]:null]]";
	   
	   System.out.print("Testing getStudentsInClass\t");
		List<ProfileResponse> response = client.getStudentsInClass(classId);
		printResults(response.toString(), expected);
	}

	public void getUserProfilesInClass() throws IOException {
	   final String expected = "[email:jnakaso@yahoo.com, ID:1bf6b74b-b7ef-4ed4-a23b-8cac2ba04417, Classes:[name:class 2, quarter:Spring 2024, assignments[]:null], email:jeffrey.nakasone@digitalaidseattle.org, ID:0de3cf6c-0464-4e7b-9d69-46b2b0162636, Classes:[name:class 2, quarter:Spring 2024, assignments[]:null]]";
		
	   System.out.print("Testing getUserProfilesInClass\t");
	   List<ProfileResponse> response = client.getUserProfilesInClass(classId, false);
		printResults(response.toString(), expected);
	}

	public void testGetUserProfile() throws IOException {
	   final String expected = "email:jeffrey.nakasone@digitalaidseattle.org, ID:0de3cf6c-0464-4e7b-9d69-46b2b0162636, Classes:[name:class 2, quarter:Spring 2024, assignments[]:null, name:CS 101, quarter:Fall 2024, assignments[]:null]";
		ProfileResponse response = client.getUserProfile(profileId);
		System.out.print("Testing getUserProfile\t");
		printResults(response.toString(), expected);
	}

	public void testGetAssignmentSubmissions() throws IOException {
		List<AssignmentSubmissionResponse> response = client.getAssignmentSubmissions(profileId, assignmentId);
		System.out.println("testGetAssignmentSubmissions " + response);
	}

	public void testGetAssignmentSubmission() throws IOException {
		Integer version = 1;
		String fileName = "Test.java";
		AssignmentSubmissionResponse response = client.getAssignmentSubmission(profileId, assignmentId, version,
				fileName);
		System.out.println("testGetAssignmentSubmission " + response);
	}

	public void testDownloadFile() throws IOException {
		String version = "1";
		String fileName = "Test.java";
		String response = client.downloadFile(profileId, assignmentId, version, fileName);
		System.out.println("testDownloadFile " + response);
	}

	public void testGetLatestSubmittedVersion() throws IOException {
		String response = client.getLatestSubmittedVersion(profileId, assignmentId);
		System.out.println("testGetLatestSubmittedVersion " + response);
	}

	public void testGetLatestSubmittedVersionFileName() throws IOException {
		String fileName = "Test2.java";

		String response = client.getLatestSubmittedVersion(profileId, assignmentId, fileName);
		System.out.println("testGetLatestSubmittedVersionFileName " + response);
	}
	
	public void testGetClass() throws IOException {
		AutograderClass response = (AutograderClass) client.getClass(classId);
		System.out.println("testGetClass " + response);
	}
	
	public void testGetSubmittedStudents() throws IOException {
		List<ProfileResponse> response = client.getSubmittedStudents(classId2, assignmentId);
		System.out.println("testGetSubmittedStudents " + response);
	}
	
	public void testGetSubmittedVersionsForAssignment() throws IOException {
		List<SubmissionResponse> response = client.getSubmittedVersionsForAssignment(classId2, assignmentId);
		System.out.println("testGetLatestSubmittedVersionFileName " + response);
	}

	public void testGetFileInputStream() throws IOException {
		String version = "1";
		String fileName = "Test.java";
		InputStream stream = client.getFileInputStream(profileId, assignmentId, version, fileName);
		System.out.println("testGetFileInputStream " + stream);
	}
	
}
