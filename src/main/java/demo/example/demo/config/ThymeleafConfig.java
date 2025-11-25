package demo.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.context.IExpressionContext;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// Custom Utility Object (Helper for the Dialect)
class CustomUtilityObject {
    
    public String formatDuration(Long totalSeconds) {
        if (totalSeconds == null || totalSeconds <= 0) {
            return "0s";
        }
        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.isEmpty()) { 
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }
    
    public String formatMonth(LocalDate date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
        return date.format(formatter);
    }
}

// Custom Dialect Definition
class CustomUtilityDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

    public CustomUtilityDialect() {
        super("CustomUtilDialect", "util", StandardDialect.PROCESSOR_PRECEDENCE);
    }

    @Override
    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        return new HashSet<>();
    }
    
    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of("util"); 
            }

            @Override
            public Object buildObject(final IExpressionContext context, final String expressionObjectName) {
                if ("util".equals(expressionObjectName)) {
                    return new CustomUtilityObject();
                }
                return null;
            }

            @Override
            public boolean isCacheable(final String expressionObjectName) {
                return true;
            }
            
            public boolean is20Compatible() {
                return true;
            }
        };
    }
}


// Spring Configuration
@Configuration
public class ThymeleafConfig {

    /**
     * Spring Boot automatically finds and adds this dialect to the 
     * auto-configured SpringTemplateEngine.
     */
    @Bean
    public CustomUtilityDialect customUtilityDialect() {
        return new CustomUtilityDialect();
    }
}