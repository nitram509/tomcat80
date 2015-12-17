
# Tomcat 8.0.x pimped with brotli compression support

Experimental revise of [Tomcat 8.0.x](http://tomcat.apache.org)
to provide Google's [brotli](https://github.com/google/brotli) compression.

It uses [jbrotli](https://github.com/nitram509/jbrotli) Java implementation.


##### Status of this project

🚨 Experimental 🚀


## How to build

Currently the build process requires manual work.
You need to have ```tomcat80``` and ```jbrotli``` checked out side by side.

```
+- your-project-folder
  |
  +- tomcat80
  +- jbrotli
```

1. Build jbrotli
   * see [README.md](https://github.com/nitram509/jbrotli/blob/master/README.md) for details
2. Build tomcat80
   * simply run ```ant``` in the project folder

Attention: currently the win32-x86-64 platform libs are hard-wired.
You may need to adopt Tomcat's ```build.xml``` to choose you correct platform.
   

## How to run

1. switch to folder ```output/build/bin```
2. as usual, run ```startup.bat``` or ```startup.sh```


