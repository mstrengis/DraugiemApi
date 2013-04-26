package draugiem.lv.api;

public class User {
	public int id, age, sex;
	public String name, surname, nick, city, lang, imageIcon, imageLarge, birthday;
	public User(int id, String name, String surname, String nick, String city, String lang, String imageIcon, String imageLarge, String birthday, int age, int sex){
		this.id = id;
		this.age = age;
		this.sex = sex; 
		this.name = name;
		this.surname = surname;
		this.nick = nick;
		this.city = city;
		this.lang = lang;
		this.imageIcon = imageIcon;
		this.imageLarge = imageLarge;
		this.birthday = birthday;
	}
	
}
