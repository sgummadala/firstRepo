package com.cvs.pos;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileReadScanner {

		    public static void main(String args[]) throws FileNotFoundException
		    {
		  
		        //creating File instance to reference text file in Java
		        File text = new File(" /Users/gummadala/Documents/SrcFiles/txt");
		      
		        //Creating Scanner instnace to read File in Java
		        Scanner scnr = new Scanner(text);
		      
		        //Reading each line of file using Scanner class
		        int lineNumber = 1;
		        while(scnr.hasNextLine()){
		            String line = scnr.nextLine();
		            System.out.println("line " + lineNumber + " :" + line);
		            lineNumber++;
		        }       
		    
		    	  
		}
	
	}


