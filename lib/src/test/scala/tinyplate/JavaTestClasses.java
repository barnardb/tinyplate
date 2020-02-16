package tinyplate;

public abstract class JavaTestClasses {

    public static class Bean {
        public int getFaa() { return 3; }
        public int getFoo() { return 5; }
        public int getFuu() { return 8; }
    }

    public static class JavaWithScalaNaming {
        public int faa() { return 3; }
        public int foo() { return 5; }
        public int fuu() { return 8; }
    }

    public static class Fields {
        public int faa = 3;
        public int foo = 5;
        public int fuu = 8;
    }

    public static class JavaDisambiguation {
        public String foo = "field";
        public String getFoo() { return "bean property"; }
        public String foo() { return "Scala-style method"; }
    }

}
