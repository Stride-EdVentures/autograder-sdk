import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import classes.AutograderClass;
import profiles.ProfileResponse;
import sdk.AutograderClient;
import storage.SubmissionResponse;
import submissions.AssignmentSubmissionResponse;

public class AutograderClientTest {

	// Note: using service key, needed to get to submissions
	AutograderClient client = new AutograderClient(
//          "https://rhojoslgtpvnlmsfppjq.supabase.co",
//			"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJob2pvc2xndHB2bmxtc2ZwcGpxIiwicm9sZSI6ImFub24iLCJpYXQiOjE2NTU1MDE0MTQsImV4cCI6MTk3MTA3NzQxNH0.GYmHNPhcL-uiKxA_ImhxvEd8iM6FyxDh3n2etfODVag");
			"https://dkcshyaivhlizysxlwbg.supabase.co",
			"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRrY3NoeWFpdmhsaXp5c3hsd2JnIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTcxMDI4Mzk1NywiZXhwIjoyMDI1ODU5OTU3fQ.Xdmi4IONsxDmsrxGTgYvnrt7CuaJBPbRLDgZT8Gfi5U");

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

	public void testGetStudentsInClass() throws IOException {
		String classId = "1090d880-e9ed-4147-95c9-a62578a63429";
		List<ProfileResponse> response = client.getStudentsInClass(classId);
		System.out.println("testGetStudentsInClass " + response);
	}

	public void getUserProfilesInClass() throws IOException {
		String classId = "1090d880-e9ed-4147-95c9-a62578a63429";
		List<ProfileResponse> response = client.getUserProfilesInClass(classId, false);
		System.out.println("getUserProfilesInClass " + response);
	}

	public void testGetUserProfile() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		ProfileResponse response = client.getUserProfile(profileId);
		System.out.println("testGetUserProfile " + response);

	}

	public void testGetAssignmentSubmissions() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
		List<AssignmentSubmissionResponse> response = client.getAssignmentSubmissions(profileId, assignmentId);
		System.out.println("testGetAssignmentSubmissions " + response);
	}

	public void testGetAssignmentSubmission() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
		Integer version = 1;
		String fileName = "Test.java";
		AssignmentSubmissionResponse response = client.getAssignmentSubmission(profileId, assignmentId, version,
				fileName);
		System.out.println("testGetAssignmentSubmission " + response);
	}

	public void testDownloadFile() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
		String version = "1";
		String fileName = "Test.java";
		String response = client.downloadFile(profileId, assignmentId, version, fileName);
		System.out.println("testDownloadFile " + response);
	}

	public void testGetLatestSubmittedVersion() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";

		String response = client.getLatestSubmittedVersion(profileId, assignmentId);
		System.out.println("testGetLatestSubmittedVersion " + response);
	}

	public void testGetLatestSubmittedVersionFileName() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
		String fileName = "Test2.java";

		String response = client.getLatestSubmittedVersion(profileId, assignmentId, fileName);
		System.out.println("testGetLatestSubmittedVersionFileName " + response);
	}
	
	public void testGetClass() throws IOException {
		String classId = "47cb01e0-ad85-4225-b46f-4899674a7159";

		AutograderClass response = (AutograderClass) client.getClass(classId);
		System.out.println("testGetClass " + response);
	}
	
	public void testGetSubmittedStudents() throws IOException {
		String classId = "47cb01e0-ad85-4225-b46f-4899674a7159";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";

		List<ProfileResponse> response = client.getSubmittedStudents(classId, assignmentId);
		System.out.println("testGetSubmittedStudents " + response);
	}
	
	public void testGetSubmittedVersionsForAssignment() throws IOException {
		String classId = "47cb01e0-ad85-4225-b46f-4899674a7159";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";

		List<SubmissionResponse> response = client.getSubmittedVersionsForAssignment(classId, assignmentId);
		System.out.println("testGetLatestSubmittedVersionFileName " + response);
	}

	public void testGetFileInputStream() throws IOException {
		String profileId = "0de3cf6c-0464-4e7b-9d69-46b2b0162636";
		String assignmentId = "cfdad040-4e03-4bf6-b816-c1f7776959cb";
		String version = "1";
		String fileName = "Test.java";
		InputStream stream = client.getFileInputStream(profileId, assignmentId, version, fileName);
		System.out.println("testGetFileInputStream " + stream);
	}
	
}
