package moon.studio.sabre.query;

public record Condition(String column, String operator, Object value) {

    public String toSql() {
        return column + " " + operator + " ?";
    }
}
