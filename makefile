.PHONY:target
target: classes
	javac -d get_bin -sourcepath . -encoding utf-8 -target 8 -source 8 Main.java

classes:
	mkdir get_bin
