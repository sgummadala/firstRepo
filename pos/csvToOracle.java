package com.cvs.pos;

import java.io.*;
import au.com.bytecode.opencsv.CSVReader;
import java.util.*;
import java.sql.*; 
public class csvToOracle {  
        public static void main(String[] args) throws Exception{                
                /* Create Connection objects */
                Class.forName ("oracle.jdbc.OracleDriver"); 
                Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@rri1rwhsds1v:1521:rwhwd0", "sgummadala", "srihita246!");
                PreparedStatement sql_statement = null;
                /* Create the insert statement */
               /* String jdbc_insert_sql = "INSERT INTO CSV_2_ORACLE"
                                + "(USER_ID, USER_AGE) VALUES"
                                + "(?,?)";
                */
                //String jdbc_insert_sql =  "INSERT INTO Student (student_id, name, email, university) VALUES (?,?,?,?)";
                // String jdbc_insert_sql =  "INSERT INTO Student_new (student_id, name) VALUES (?,?)";
                String jdbc_insert_sql =  "INSERT INTO Student_new (student_id, name, email, university) VALUES (?,?,?,?)";
                //String jdbc_insert_sql =  "INSERT INTO Student_new (student_id) VALUES (?)";
                sql_statement = conn.prepareStatement(jdbc_insert_sql);
                /* Read CSV file in OpenCSV */
                String inputCSVFile = "C:/Java/Data/inputdata.csv";
                
                CSVReader reader = new CSVReader(new FileReader(inputCSVFile));
                /* Variables to loop through the CSV File */
                String [] nextLine; /* for every line in the file */            
               // int lnNum = 0; /* line number */
                while ((nextLine = reader.readNext()) != null) {
                       // lnNum++;
                        /* Bind CSV file input to table columns */
                        sql_statement.setString(1, nextLine[0]);
                        /* Bind Age as double */
                        /* Need to convert string to double here */
                        //sql_statement.setDouble(2,Double.parseDouble(nextLine[1]));
                        sql_statement.setString(2, nextLine[1]);
                        sql_statement.setString(3, nextLine[2]);
                        sql_statement.setString(4, nextLine[3]);
                        
                        //sql_statement.setDouble(3,Double.parseDouble(nextLine[2]));
                        //sql_statement.setDouble(4,Double.parseDouble(nextLine[3]));
                        
                        /* execute the insert statement */
                        sql_statement.executeUpdate();
                }               
                /* Close prepared statement */
                sql_statement.close();
                /* COMMIT transaction */
                conn.commit();
                /* Close connection */
                conn.close();
        }
}
