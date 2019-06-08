.PHONY:target
target: classes
	javac -d classes Main.java -encoding utf-8 -target 8 -source 8

classes:
	mkdir classes
