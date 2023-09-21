package org.acme.lego.validate;
record ValidationResult(int epoch, double accuracy) implements Comparable {

    @Override
    public int compareTo(Object other) {
        if (other instanceof ValidationResult o) {
            return Double.compare(o.accuracy, accuracy);
        }
        return 0;
    }
}
