package io.p6spy.formatter;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.util.Locale;
import java.util.Stack;

public class P6spyPrettySqlFormatter implements MessageFormattingStrategy {
    
    @Override
    public String formatMessage(final int connectionId, final String now, final long elapsed, final String category, final String prepared, final String sql, final String url) {
        Stack<String> callStack = new Stack<>();
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        for(StackTraceElement stackTraceElement : stackTrace) {
            if(stackTraceElement.toString().startsWith("io.p6spy") && !stackTraceElement.toString().contains("P6spyPrettySqlFormatter")) {
                callStack.push(stackTraceElement.toString());
            }
        }
        StringBuilder callStackBuilder = new StringBuilder();
        int order = 1;
        while(callStack.size() != 0) {
            callStackBuilder.append("\n\t\t" + (order++) + ". " + callStack.pop());
        }
        return format(connectionId, callStackBuilder.toString(), elapsed, category, sql);
    }
    
    private String format(final int connectionId, final String callStack, final long elapsed, final String category, String sql) {
        if(sql.trim() == null || sql.trim().isEmpty()) {
            return "";
        }
        
        if(Category.STATEMENT.getName().equals(category)) {
            String s = sql.trim().toLowerCase(Locale.ROOT);
            if("create".startsWith(s) || "alter".startsWith(s) || "comment".startsWith(s)) {
                sql = FormatStyle.DDL
                        .getFormatter()
                        .format(sql);
            }
            else {
                sql = FormatStyle.BASIC
                        .getFormatter()
                        .format(sql);
            }
        }
        
        return new StringBuilder()
                .append(sql.toUpperCase())
                .append("\n\n\tConnection ID: ").append(connectionId)
                .append("\n\tExecution Time: ").append(elapsed).append(" ms\n")
                .append("\n\tCall Stack (number 1 is entry point): ").append(callStack).append("\n")
                .append("\n-----------------------------------------------------------------------------------------------------------------------------------------------------")
                .toString();
    }
    
}