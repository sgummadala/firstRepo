package com.cvs.pos;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Scanner;
 

public class StudentsInsert {
 
   @SuppressWarnings("resource")
    public static void main(String[] args) {
 
        // Initialising database and connection objects:
        //String dbURL = "jdbc:mysql://localhost:3306/Students";
    	String dbURL = "jdbc:oracle:thin:@rri1rwhsds1v:1521:rwhwd0";
        String username = "sgummadala";
        String password = "srihita246!";
 
        Connection conn = null;
        String sql = null;
 
        try {
 
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the number of students: ");
 
            int numberOfStudents = scanner.nextInt();
            int countInsertion = 0;
            int countInformationToDisplay = 1;
 
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
 
            while (countInsertion < numberOfStudents) {
                System.out.println("Enter information for student #" + countInformationToDisplay + "\t");
                System.out.print("");
                
                System.out.print("\tID: ");
                String id = reader.readLine();
                
                System.out.print("\tName: ");
                String name = reader.readLine();
                
                System.out.print("\tEmail: ");
                String email = reader.readLine();
                
                System.out.print("\tUniversity: ");
                String university = reader.readLine();
 
                conn = DriverManager.getConnection(dbURL, username, password);
 
                // Inserting data to the Student table
                sql = "INSERT INTO Student (student_id, name, email, university) VALUES (?,?,?, ?)";
                PreparedStatement statement = conn.prepareStatement(sql);
                statement.setString(1, id);
                statement.setString(2, name);
                statement.setString(3, email);
                statement.setString(4, university);
 
                int rowInserted = statement.executeUpdate();
 
                if (rowInserted > 0) {
                    ++countInsertion;
                    ++countInformationToDisplay;
                }
 
                System.out.println("" + countInsertion + " students saved to database.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}