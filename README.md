# P6spy
쿼리 튜닝을 쾌적하게 ! P6spy 개조하기

## **🚫 주의!**

-   **굉장히 비싼 자원을 사용하므로 운영 환경에선 절대 사용하지 말 것을 권장 드립니다.**

---

## **✅ 개발 환경**

-   **Java 11**
-   **Gradle 7.0.2**
-   **Spring-Boot 2.5.0**
-   **Spring-Data-JPA**
-   **H2 Database**
-   **p6spy 1.7.1**

---

## **✅ 필수 설정**

```
//build.gradle
implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.7.1'
```

```
//application.yaml
decorator:
  datasource:
    p6spy:
      enable-logging: true
```

---

`JPA`를 쓰다 보면 예상 밖의 쿼리가 발생하는 경우가 굉장히 많다.

일단, `JPA`를 쓰지 않더라도 쿼리를 파악하기 좋게 해주는 라이브러리로 `p6spy`가 있는데,

기본값으로 사용할 경우 생각보다 가독성이 좋지 않다.

왜냐하면 쿼리가 한번 발생하면

파라미터가 바인딩되지 않은 원본 쿼리와

파라미터를 바인딩한 후의 쿼리

총 두 개의 쿼리가 나란히 출력되기 때문이다.


<img src="https://blog.kakaocdn.net/dn/OqVeK/btq5002NBXw/6CyKDoYPDaTt2EidF8iqX1/img.png" />


이처럼 간단한 쿼리의 경우는 괜찮을 수 있으나,

통계성 쿼리 같은 복잡하고 수십 줄 이상의 빅 쿼리가 두 개 연달아 나오면 굉장히 혼란스럽다.

그래서 쿼리를 파악하기 쉽게 하기 위해 p6spy를 커스터마이징했다.

---

### **📕 p6spy는 대략 다음과 같은 과정을 거쳐 쿼리를 캡처한다.**


<img src="https://blog.kakaocdn.net/dn/TWMZf/btq54VrLzro/P86PEEFjvqKIPMh4PEoo81/img.png" />


1\. `DataSource`를 래핑한 `ResultSetWrapper`(프락시)를 만든다.

2\. 쿼리가 발생하여 JDBC가 `ResultSet`을 반환하면 이를 만들어둔 프락시로 가로챈다.

3\. 내부적으로 이런저런 정보들을 수집하고 p6spy의 옵션을 적용해준다.

4\. `Slf4j`를 사용해 로깅한다.

---

이때 쿼리를 포매팅하는 Default 객체가 `MultiLineFormat`이다.

```
public class P6SpyProperties {

    /**
     * Enables logging JDBC events.
     *
     * @see com.p6spy.engine.logging.P6LogFactory
     */
    private boolean enableLogging = true;
    /**
     * Enables multiline output.
     */
    private boolean multiline = true;
    /**
     * Logging to use for logging queries.
     */
    private P6SpyLogging logging = P6SpyLogging.SLF4J;
    /**
     * Name of log file to use (only with logging=file).
     */
    private String logFile = "spy.log";
    /**
     * Custom log format.
     */
    private String logFormat;
}
```

private String logFormat은 기본값이 null이며

```
if (!initialP6SpyOptions.containsKey("logMessageFormat")) {
            if (p6spy.getLogFormat() != null) {
                System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.CustomLineFormat");
                System.setProperty("p6spy.config.customLogMessageFormat", p6spy.getLogFormat());
            }
            else if (p6spy.isMultiline()) {
                System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.MultiLineFormat");
            }
        }
```

null이기 때문에 `CustomLogMessageFormat`이 아닌 `MultiLineFormat`으로 타고 들어간다.

이후 `MultiLineFormat`의 포맷을 보면

```
public class MultiLineFormat implements MessageFormattingStrategy {

  @Override
  public String formatMessage(final int connectionId, final String now, final long elapsed, final String category, final String prepared, final String sql, final String url) {
    return "#" + now + " | took " + elapsed + "ms | " + category + " | connection " + connectionId + "| url " + url + "\n" + prepared + "\n" + sql +";";
  }
  
}
```


<img src="https://blog.kakaocdn.net/dn/OqVeK/btq5002NBXw/6CyKDoYPDaTt2EidF8iqX1/img.png" />


이러한 결과가 나타난다.

코드를 보면 알겠지만, 원하는 포맷으로 확장하기 위해서 `CustomLogMessageFormat`을 구현하여 지정해주면 된다.

```
@Configuration
public class P6spyConfig {
    
    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6spyPrettySqlFormatter.class.getName());
    }
    
}
```

설정 클래스를 생성하여 새로운 LogFormatter를 지정해준 후 구현에 들어간다.

```
public class P6spyPrettySqlFormatter implements MessageFormattingStrategy {
    
    @Override
    public String formatMessage(final int connectionId, final String now, final long elapsed, final String category, final String prepared, final String sql, final String url) {
        return null;
    }
    
}
```

`MessageFormattingStrategy(메시지 포맷 전략)`을 구현한다.

우선 해당 쿼리가 정확히 어떤 경로를 타고 발생했는지 기록해줄 것이다.

```
StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    for(StackTraceElement stackTraceElement : stackTrace) {
        System.out.println(stackTraceElement);
    }
```

`Throwable`을 호출하여 `stackTrace`를 쭉 뽑아보면

```
io.p6spy.formatter.P6spyPrettySqlFormatter.formatMessage(P6spyPrettySqlFormatter.java:15)
com.p6spy.engine.spy.appender.Slf4JLogger.logSQL(Slf4JLogger.java:50)
com.p6spy.engine.common.P6LogQuery.doLog(P6LogQuery.java:121)
com.p6spy.engine.common.P6LogQuery.doLogElapsed(P6LogQuery.java:91)
com.p6spy.engine.common.P6LogQuery.logElapsed(P6LogQuery.java:203)
com.p6spy.engine.logging.LoggingEventListener.logElapsed(LoggingEventListener.java:107)
com.p6spy.engine.logging.LoggingEventListener.onAfterCommit(LoggingEventListener.java:54)
com.p6spy.engine.event.CompoundJdbcEventListener.onAfterCommit(CompoundJdbcEventListener.java:285)
com.p6spy.engine.wrapper.ConnectionWrapper.commit(ConnectionWrapper.java:172)
org.hibernate.resource.jdbc.internal.AbstractLogicalConnectionImplementor.commit(AbstractLogicalConnectionImplementor.java:86)
org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.commit(JdbcResourceLocalTransactionCoordinatorImpl.java:282)
org.hibernate.engine.transaction.internal.TransactionImpl.commit(TransactionImpl.java:101)
org.springframework.orm.jpa.JpaTransactionManager.doCommit(JpaTransactionManager.java:562)
org.springframework.transaction.support.AbstractPlatformTransactionManager.processCommit(AbstractPlatformTransactionManager.java:743)
org.springframework.transaction.support.AbstractPlatformTransactionManager.commit(AbstractPlatformTransactionManager.java:711)
org.springframework.transaction.interceptor.TransactionAspectSupport.commitTransactionAfterReturning(TransactionAspectSupport.java:654)
org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:407)
org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:137)
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor$CrudMethodMetadataPopulatingMethodInterceptor.invoke(CrudMethodMetadataPostProcessor.java:174)
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:97)
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:215)
com.sun.proxy.$Proxy88.save(Unknown Source)
io.p6spy.controller.MainController.run(MainController.java:35)
java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
java.base/java.lang.reflect.Method.invoke(Method.java:566)
org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:197)
org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:141)
org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:106)
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:894)
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:808)
org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1063)
org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:963)
org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1006)
org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:909)
javax.servlet.http.HttpServlet.service(HttpServlet.java:652)
org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883)
javax.servlet.http.HttpServlet.service(HttpServlet.java:733)
org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:227)
org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53)
org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:189)
org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:162)
org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)
org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97)
org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542)
org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:143)
org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)
org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78)
org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:357)
org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:374)
org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)
org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:893)
org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1707)
org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
java.base/java.lang.Thread.run(Thread.java:834)
2021-05-28 21:31:46.316  INFO 2820 --- [nio-8080-exec-2] p6spy                                    : 

```

`Throwable`이 호출된 시점까지의 모든 경로가 출력된다.

여기서 필요한 부분만 추출하면 되는데, 원하는 trace에서 공통점을 찾을 수 있다.

**바로 문자열의 시작점이 main 패키지의 경로라는 것이다.**

따라서 아래와 같이 코드를 작성해준다면...

```
for(StackTraceElement stackTraceElement : stackTrace) {
        if(stackTraceElement.toString().startsWith("io.p6spy")) {
            System.out.println(stackTraceElement);
        }
    }
```


<img src="https://blog.kakaocdn.net/dn/bgRRsF/btq52BnMaFm/G1xpyThrgjfyc2jLfzx71k/img.png" />


이러한 로그를 얻을 수 있다.

이 로그를 보고 알 수 있는 것은

`MainController`의 run 메서드가 호출됐고 이후 `call_1`~`call_3` 메서드가 연쇄 호출되며 쿼리가 발생했다는 것이다.

`MainControlelr`의 코드를 보자.

```
@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @ResponseBody
    @PostMapping("/matriculation")
    public void run(MatriculationRequest matriculationRequest) {
        Student student = matriculationRequest.getStudent();
        School school = schoolRepository.findByName(matriculationRequest.getSchoolName());
        if(school == null) {
            school = schoolRepository.save(createSchool());
        }
        school.matriculation(student);
        call_1(student);
    }
    
    private School createSchool() {
        return School.builder()
                     .name("용산초")
                     .build();
    }
    
    private void call_1(Student student) {
        call_2(student);
    }
    
    private void call_2(Student student) {
        call_3(student);
    }
    
    private void call_3(Student student) {
        studentRepository.save(student);
    }
    
}
```

```
StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    for(StackTraceElement stackTraceElement : stackTrace) {
            if(stackTraceElement.toString().startsWith("io.p6spy") && !stackTraceElement.toString().contains("P6spyPrettySqlFormatter")) {
                System.out.println(stackTraceElement);
            }
        }
```

여기서 `P6spyPrettySqlFormatter`의 `trace`는 필요 없기 때문에 필터링 해 준다.


<img src="https://blog.kakaocdn.net/dn/bMPuxL/btq50ZbTTF5/NFzG0PdGYpBhp57HC1nnK0/img.png" />


원하는 결과만 나온다.

이제 이 로그를 더 보기 편하게 역순으로 뒤집어줄 것이다.

Stack을 활용할 것인데, 추출되는 trace를 순서대로 Stack에 push 하고, 다시 pop 하면 역순으로 뒤집힐 것이다.

```
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
    return null;
}
```


<img src="https://blog.kakaocdn.net/dn/b8jty3/btq52SW9y1L/V5yBIs5gfS5gXcDpcSRE4K/img.png" />


쿼리가 발생한 지점과 클릭하면 즉시 이동할 수 있는 포탈(🤗)이 생성됐다.

이제 SQL을 보기 좋게 포매팅 할 것이다.

`formatMessage` 메서드에 이런저런 파라미터가 많이 들어오는데

이에 대한 자세한 내용은 `p6spy docs`를 보면 하기와 같다. 

```
Params:
connectionId – the id of the connection
now – the current ime expressing in milliseconds
elapsed – the time in milliseconds that the operation took to complete
category – the category of the operation
prepared – the SQL statement with all bind variables replaced with actual values
sql – the sql statement executed
url – the database url where the sql statement executed
```

이 파라미터들을 적당히 버무려 준다.

```
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
```

<img src="https://blog.kakaocdn.net/dn/dmtdA3/btq52biODsp/PRr2duWhzoktw11PVf2uV0/img.png" />
