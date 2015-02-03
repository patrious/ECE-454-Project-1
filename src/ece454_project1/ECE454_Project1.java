package ece454_project1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ECE454_Project1 {

    public static void main(String[] args) throws IOException {
        int myPort = Integer.parseInt(args[0]);
        FileManager fm = new FileManager();
        NetworkManager nm = new NetworkManager();
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        boolean started = false;
        while (true) {

            String input = bufferRead.readLine();
            if ("query".equals(input)) {
                if (started) {
                    Status ret = fm.query();
                    System.out.println(ret);
                } else {
                    System.out.println("Start System first");
                }
            } else if ("exit".equals(input)) {
                started = false;
                fm.Leave();
                nm.leave();
                break;

            } else if ("join".equals(input)) {
                started = true;
                fm.StartFileManager(myPort);
                nm.StartManager(fm, myPort);
            } else if ("leave".equals(input)) {
                started = false;
                fm.Leave();
                nm.leave();
            } else if ("insert".equals(input)) {
                if (started) {
                    System.out.println("Please Input filename:");
                    input = bufferRead.readLine();
                    fm.Insert(input);
                } else {
                    System.out.println("Start System first");
                }
            } else {
                System.out.println("Enter Something else");
            }
        }
    }

    private static void leave() {
    }
}
