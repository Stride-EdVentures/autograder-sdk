import authentication.AuthenticationResponse;
import sdk.AutograderClient;

import java.io.IOException;

public class AutograderClientTest {

    public static void main(String[] args) throws IOException {
        AutograderClient client = new AutograderClient(
                "https://rhojoslgtpvnlmsfppjq.supabase.co",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJob2pvc2xndHB2bmxtc2ZwcGpxIiwicm9sZSI6ImFub24iLCJpYXQiOjE2NTU1MDE0MTQsImV4cCI6MTk3MTA3NzQxNH0.GYmHNPhcL-uiKxA_ImhxvEd8iM6FyxDh3n2etfODVag"
        );

        AuthenticationResponse response = client.authenticateUser("test@gmail.com", "test1234");
        boolean isSuccessful = client.inviteTeacher("revanthp@mit.edu");
        System.out.println(isSuccessful);
    }
}
