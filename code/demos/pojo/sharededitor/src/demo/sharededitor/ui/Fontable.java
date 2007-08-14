/*
@COPYRIGHT@
 */
package demo.sharededitor.ui;

public interface Fontable {
	void setFontInfo(String name, int size, String text);

	void setFontName(String name);

	void setFontSize(int size);

	void setText(String text);

	String getText();

	void appendToText(char c);
}
