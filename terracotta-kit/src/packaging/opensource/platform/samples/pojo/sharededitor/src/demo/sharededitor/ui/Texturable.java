/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.ui;

import java.awt.Image;

public interface Texturable {
	void setTexture(Image image);

	void clearTexture();
}
