# Material Number Sliding Picker (Fork)

A widget that enables the user to select a number from a predefined range.
Progress value can be changed using the up and down arrows, click and edit the editable text or swiping up/down or left/right.

<img src="./art/video.gif" alt="Screen shot" width="50%"/>

[![Build Status](https://travis-ci.org/sephiroth74/NumberSlidingPicker.svg?branch=master)](https://travis-ci.org/sephiroth74/NumberSlidingPicker) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/it.sephiroth.android.library/number-sliding-picker/badge.svg)](https://maven-badges.herokuapp.com/maven-central/it.sephiroth.android.library/number-sliding-picker) [![](https://jitpack.io/v/sephiroth74/NumberSlidingPicker.svg)](https://jitpack.io/#sephiroth74/NumberSlidingPicker)

# About this Fork

I've forked this project to remove _RxJava_ dependency since we can achieve the same result using coroutines.

# Installation

## Maven

```gradle
compile 'it.sephiroth.android.library:number-sliding-picker:**version**'
```	
	
## JitPack

### Step 1. Add the JitPack repository to your build file:

```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

### Step 2. Add the dependency

```gradle
dependencies {
        implementation 'com.github.sephiroth74:NumberSlidingPicker:Tag'
}
```

Get the latest version  on [JitPack](https://jitpack.io/#sephiroth74/NumberSlidingPicker)	

# Usage

```xml
    <it.sephiroth.android.library.numberpicker.NumberPicker
        style="@style/NumberPicker.Filled"
        app:picker_max="100"
        app:picker_min="0"
        android:progress="50"
        app:picker_stepSize="2"
        app:picker_tracker="exponential"
        app:picker_orientation="vertical"
        android:id="@+id/numberPicker"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        ... />
```

> See [attrs.xml](./numberpicker/src/main/res/values/attrs.xml) for a complete list of attributes

## Listener

```kotlin
        numberPicker.doOnProgressChanged { numberPicker, progress, formUser ->
            // progress changed
        }
        
        numberPicker.doOnStartTrackingTouch { numberPicker -> 
            // tracking started
        }
        
        numberPicker.doOnStopTrackingTouch { numberPicker -> 
            // tracking ended
        }
```

# License
MIT License

Copyright (c) 2019 Alessandro Crugnola

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
