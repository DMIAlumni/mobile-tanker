package me.mariotti.ai;


import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface IBaseAi {
    void targetPosition(Mat frame, Rect target, int position);
    void think();
}
