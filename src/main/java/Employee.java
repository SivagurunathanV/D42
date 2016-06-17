/**
 * Created by sivagurunathan.v on 16/06/16.
 */
public class Employee {
  public int number;
  public String name;
  public String gender;

  public String getGender() {
	return gender;
  }

  public void setGender(String gender) {
	this.gender = gender;
  }

  public String getName() {
	return name;
  }

  public void setName(String name) {
	this.name = name;
  }

  public int getNumber() {

	return number;
  }

  public void setNumber(int number) {
	this.number = number;
  }

  @Override
  public String toString() {
	return "Name," + name + ",Gender," + gender + ",number," + number;
  }
}
