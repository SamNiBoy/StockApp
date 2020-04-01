package com.sn.draw;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public interface Draw {
    public String getTitle();
    public int getWidth();
    public int getHeight();
    public String getXLabel();
    public String getYLabel();
    public BufferedImage Draw();
}
