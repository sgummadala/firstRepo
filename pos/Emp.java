package com.cvs.pos;

public class Emp 
{
//Variables declare
	String name;
	int age;
	String position;
	double salary;
	
//Constructor
	public Emp(String empname)
	{
		this.name = empname;
	}
	
//Method1
	public void setAge(int empage)
	{
		age =  empage;
	}

//Method2
	public void setPosition(String empposition)
		{
			position =  empposition;
		}
	
//Method3
	public void setSalary(double empsalary)
			{
				salary =  empsalary;
			}
	
//Method4
    public void printEmp() {
	      System.out.println("Name:"+ name );
	      System.out.println("Age:" + age );
	      System.out.println("Designation:" + position );
	      System.out.println("Salary:" + salary);
	      System.out.println("Test");
	   }

}
