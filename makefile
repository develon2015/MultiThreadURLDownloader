.PHONY:target
target: classes
	javac -d classes -sourcepath . -encoding utf-8 -target 8 -source 8 Main.java

classes:
	mkdir classes
