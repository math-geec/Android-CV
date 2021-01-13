# Android-CV

Style Transfer app based on the idea in [A Neural Algorithm of Artistic Style](https://arxiv.org/abs/1508.06576).
####

Two potential approaches:
* Model on device, i.e. deploy the model in the app with TensorFlow Lite. Work in branch [TFlite](https://github.com/math-geec/Android-CV/tree/TFlite).
* Connect external model through API request. Work in branch [pythonAPI](https://github.com/math-geec/Android-CV/tree/pythonAPI).

---

## TFlite

### Status: In Progress

This is a rebuild based on the pre-trained style transfer TensorFlow Lite model that is optimized for mobile. The process for optimizing the large TensorFlow model for mobile deployment is explained in the official blog [Optimizing style transfer to run on mobile with TFLite](https://blog.tensorflow.org/2020/04/optimizing-style-transfer-to-run-on-mobile-with-tflite.html).


This Artistic Style Transfer model consists of two submodels:

1. Style Prediction Model: A MobilenetV2-based neural network that takes an input style image to a 100-dimension style bottleneck vector.
2. Style Transform Model: A neural network that takes apply a style bottleneck vector to a content image and creates a stylized image.

**Note**: Since running style transfer on the UI thread is computational expensive, ViewModel and Coroutine are applied to run it on a dedicated background thread.

---

## pythonAPI

### Status: Testing upload image

*Known issue*:
- cuda = 0 kills all the RAM
- cuda = 1 runs out of drive space

*Possible solution*: reduce content image size 

Neural Style Transfer model is implemented in [repo](https://github.com/math-geec/Neural-Style-Transfer)
