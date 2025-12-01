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
// NEW IMPORTS REQUIRED FOR NUMBER FORMATTING
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Custom Utility Object (Helper for the Dialect)
class CustomUtilityObject {
    
    // EXISTING METHOD: Formats duration (e.g., 1h 5m 10s)
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
    
    // EXISTING METHOD: Formats month (e.g., November 2025)
    public String formatMonth(LocalDate date) {
        if (date == null) {
            return "";
        }
        // Using Locale.US to ensure a consistent output pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US); 
        return date.format(formatter);
    }
    
    // NEW METHOD: Formats number with thousands separator (e.g., 10000 -> 10,000)
    public String formatNumber(Object number) {
        if (number == null) {
            return "0";
        }
        
        // Ensure the object is treated as a long, casting if necessary
        long numValue;
        if (number instanceof Number) {
            numValue = ((Number) number).longValue();
        } else {
            try {
                numValue = Long.parseLong(number.toString());
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }

        // Use a DecimalFormat with US locale to reliably use comma (,) as the separator
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(numValue);
    }
}

// Custom Dialect Definition (Unchanged but complete)
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
                    // This is where the utility object is instantiated
                    return new CustomUtilityObject();
                }
                return null;
            }

            @Override
            public boolean isCacheable(final String expressionObjectName) {
                return true;
            }
            
            // This method is generally for compatibility, often omitted in modern dialects
            public boolean is20Compatible() {
                return true;
            }
        };
    }
}


// Spring Configuration (Unchanged but complete)
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