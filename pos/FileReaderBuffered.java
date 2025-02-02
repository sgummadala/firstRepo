package com.cvs.pos;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileReaderBuffered {

	
	    public static void main(String[] args) {
	        try {
	           // FileReader reader = new FileReader("C:/Java/Data/MyFile.txt");
	           //BufferedReader bufferedReader = new BufferedReader(reader);
	          //or
	        	
	            //BufferedReader bufferedReader = new BufferedReader( new FileReader("C:/Java/Data/MyFile.txt"));
	        	BufferedReader bufferedReader = new BufferedReader( new FileReader("/Users/gummadala/Documents/SrcFiles/"));
	 
	            String line;
	 
	            while ((line = bufferedReader.readLine()) != null) {
	                System.out.println(line);
	            }
	           // reader.close();
	            //or
	            bufferedReader.close();
	            
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	 
	}
	
	