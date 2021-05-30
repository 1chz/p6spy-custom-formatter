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
    
        for(int i = 0; i < stackTrace.length; i++) {
            String trace = stackTrace[i].toString();
            if(trace.startsWith("io.p6spy")) {
                callStack.push(trace);
            }
        }
    
        StringBuilder callStackBuilder = new StringBuilder();
        int order = 1;
        while(callStack.size() != 0) {
            callStackBuilder.append("\n\t\t" + (order++) + ". " + callStack.pop());
        }
    
        String message = new StringBuilder().append("\n\n\tConnection ID: ").append(connectionId)
                                            .append("\n\tExecution Time: ").append(elapsed).append(" ms\n")
                                            .append("\n\tCall Stack (number 1 is entry point): ").append(callStackBuilder).append("\n")
                                            .append("\n----------------------------------------------------------------------------------------------------")
                                            .toString();
    
        return sqlFormat(sql, category, message);
    }
    
    private String sqlFormat(String sql, String category, String message) {
        if(sql.trim() == null || sql.trim().isEmpty()) {
            return "";
        }
        
        if(Category.STATEMENT.getName().equals(category)) {
            String s = sql.trim().toLowerCase(Locale.ROOT);
            if(s.startsWith("create") || s.startsWith("alter") || s.startsWith("comment")) {
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
        
        return new StringBuilder().append("\n")
                                  .append(sql.toUpperCase())
                                  .append(message)
                                  .toString();
    }
    
}
