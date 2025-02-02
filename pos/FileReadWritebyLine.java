package com.cvs.pos;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class FileReadWritebyLine {

	public static void main(String[] args) {
			
        try {
	         	
	          BufferedReader bufferedReader = new BufferedReader( new FileReader("C:/Java/Data/MyFile.txt"));
              String line;
              
            //  FileWriter writer = new FileWriter("MyFile.txt", true);
              BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:/Java/Data/Out_MyFile.txt", true));
	 
	            while ((line = bufferedReader.readLine()) != null) {
	                //System.out.println(line);
	            	 bufferedWriter.write(line);
	            	 bufferedWriter.newLine();
	            	
	            }
	            bufferedReader.close();
	            bufferedWriter.close();
	            
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
		

	}


