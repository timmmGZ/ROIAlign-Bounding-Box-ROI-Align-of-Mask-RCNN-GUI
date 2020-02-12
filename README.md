# Updating soon for Digit-Recognition-CNN-and-ANN-using-Mnist-with-GUI
https://github.com/timmmGZ/Digit-Recognition-CNN-and-ANN-using-Mnist-with-GUI  
I am going to modify the above program in my free time, add this ROI Align layer in it, make it become a MNIST object detection.
## ROIAlign-ROI-Align-of-Mask-RCNN-GUI
Use ROI-Align because scanning e.g. 2000 Bounding-boxes(Region-Proposals) in each image, reshaping all the 2000 sub-images inside all Bounding-boxes to the same size(e.g. 28 * 28), using CNN(or ANN) to classify all the sub images will consume a lot of time(if use ANN then will be faster a bit, but it sacrifices accuracy), so the FPS will be very low, can even be 0.5 FPS depend on your settings.  
So what ROI-Align does is as you see in the below picture, we use CNN to "reshape" the original image to a small size, it also extracts the features of the image in the meanwhile, and then scan same Bounding-boxes(e.g. 2000), for each Bounding-box, we don't need to reshape the sub-images inside the Bounding-box, instead, we use ROI-Align to transform the sub-image into output as same size(e.g. 7 * 7), and then we can use this output in Neural Network model directly to classify whether the sub-image in this Bounding-box is positive(foreground) or negative(background), so we don't need to do 2000 CNN per image, but only 1 CNN per image, which saves a lots of time and make FPS higher.
## Two examples of 5 * 5 and 7 * 7 output size
![image](https://github.com/timmmGZ/ROIAlign-ROI-Align-of-Mask-RCNN-GUI/blob/master/images/ROIexample1.jpg)
![image](https://github.com/timmmGZ/ROIAlign-ROI-Align-of-Mask-RCNN-GUI/blob/master/images/ROIexample2.jpg)
## Following example shows how Bounding-box scanning works
![image](https://github.com/timmmGZ/ROIAlign-ROI-Align-of-Mask-RCNN-GUI/blob/master/images/ROIcats.gif)
## Use only one size(Anchor) of Bounding-box and slowing down the scanning speed to see it more clearly
![image](https://github.com/timmmGZ/ROIAlign-ROI-Align-of-Mask-RCNN-GUI/blob/master/images/ROI9.gif)
