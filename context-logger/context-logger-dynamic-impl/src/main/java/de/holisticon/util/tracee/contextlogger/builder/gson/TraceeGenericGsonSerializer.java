package de.holisticon.util.tracee.contextlogger.builder.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.holisticon.util.tracee.Tracee;
import de.holisticon.util.tracee.TraceeLogger;
import de.holisticon.util.tracee.contextlogger.api.TraceeContextLogProvider;
import de.holisticon.util.tracee.contextlogger.data.subdata.NameObjectValuePair;
import de.holisticon.util.tracee.contextlogger.data.subdata.NameStringValuePair;
import de.holisticon.util.tracee.contextlogger.profile.ProfileSettings;
import de.holisticon.util.tracee.contextlogger.utility.ListUtilities;
import de.holisticon.util.tracee.contextlogger.utility.RecursiveReflectionToStringStyle;
import de.holisticon.util.tracee.contextlogger.utility.TraceeContextLogAnnotationUtilities;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic serializer for context logging output.
 * Handles field order and applies Profiles (suppresses output)
 * Created by Tobias Gindler on 14.03.14.
 */
public class TraceeGenericGsonSerializer implements JsonSerializer<Object> {

    private final static Set<Class> IGNORED_AT_DESERIALIZATION = new HashSet<Class>();

    static {
        IGNORED_AT_DESERIALIZATION.add(String.class);
        IGNORED_AT_DESERIALIZATION.add(Boolean.class);
        IGNORED_AT_DESERIALIZATION.add(Integer.class);
        IGNORED_AT_DESERIALIZATION.add(Double.class);
        IGNORED_AT_DESERIALIZATION.add(Long.class);
        IGNORED_AT_DESERIALIZATION.add(Float.class);
        IGNORED_AT_DESERIALIZATION.add(Byte.class);
    }

    private final TraceeLogger LOGGER = Tracee.getBackend().getLoggerFactory().getLogger(TraceeGenericGsonSerializer.class);

    private final ProfileSettings profileSettings;

    public TraceeGenericGsonSerializer (final ProfileSettings profileSettings) {
        this.profileSettings = profileSettings;
    }

    @Override
    public JsonElement serialize(Object instance, Type type, JsonSerializationContext jsonSerializationContext) {

        JsonObject result;

        TraceeContextLogProvider annotation = TraceeContextLogAnnotationUtilities.getAnnotationFromType(instance);
        if (annotation == null) {

            // to be ignored
            LOGGER.debug("TRACEE-CONTEXTLOGGER-GSON-SERIALIZER - Got non annotated class");
            result = new JsonObject();

        } else {

            // get those annotated methods
            final List<MethodAnnotationPair> entriesToPrint = TraceeContextLogAnnotationUtilities.getAnnotatedMethodsOfInstance(instance);

            // sort those methods
            Collections.sort(entriesToPrint, new MethodAnnotationPairComparator());

            // now create json object and add those items
            result = new JsonObject();


            for (MethodAnnotationPair singleEntry : entriesToPrint) {

                if (!singleEntry.shouldBeProcessed(profileSettings)) {
                    continue;
                }

                try {

                    Object returnValue = singleEntry.getMethod().invoke(instance, null);

                    if (TraceeContextLogAnnotationUtilities.isFlatable(singleEntry.getMethod()) && (isNameStringValuePair(returnValue))) {

                        // returnValue is single NameStringValuePair
                        final NameStringValuePair nameStringValuePair = (NameStringValuePair) returnValue;
                        result.add(nameStringValuePair.getName(), jsonSerializationContext.serialize(nameStringValuePair.getValue()));

                    }
                    if (TraceeContextLogAnnotationUtilities.isFlatable(singleEntry.getMethod()) && (isNameObjectValuePair(returnValue))) {

                        // returnValue is single NameObjectValuePair
                        final NameObjectValuePair nameObjectValuePair = (NameObjectValuePair) returnValue;

                        //  ObjectValuePairs will be deserialized by ReflectionToStringBuilder
                        final Object value = getValueOfNameObjectValuePair(nameObjectValuePair);
                        result.add(nameObjectValuePair.getName(), jsonSerializationContext.serialize(value));

                    } else if (TraceeContextLogAnnotationUtilities.isFlatable(singleEntry.getMethod()) && ListUtilities.isListOfType(returnValue, NameStringValuePair.class)) {

                        // returnValue is List of NameValuePairs
                        final List<NameStringValuePair> list = (List<NameStringValuePair>) returnValue;

                        for (NameStringValuePair nameStringValuePair : list) {
                            result.add(nameStringValuePair.getName(), jsonSerializationContext.serialize(nameStringValuePair.getValue()));
                        }

                    } else if (TraceeContextLogAnnotationUtilities.isFlatable(singleEntry.getMethod()) && ListUtilities.isListOfType(returnValue, NameObjectValuePair.class)) {

                        // returnValue is List of NameValuePairs
                        List<NameObjectValuePair> list = (List<NameObjectValuePair>) returnValue;

                        for (NameObjectValuePair nameObjectValuePair : list) {
                            //  ObjectValuePairs will be deserialized by ReflectionToStringBuilder
                            final Object value = getValueOfNameObjectValuePair(nameObjectValuePair);
                            result.add(nameObjectValuePair.getName(), jsonSerializationContext.serialize(value));
                        }

                    } else {

                        // returnValue is not a NameStringValuePair == static context data
                        result.add(singleEntry.getAnnotation().displayName(), jsonSerializationContext.serialize(returnValue));

                    }

                } catch (Exception e) {

                    // to be ignored
                    LOGGER.debug("TRACEE-CONTEXTLOGGER-GSON-SERIALIZER - Exception during serialization.", e);

                }

            }

        }

        return result;
    }

    /**
     * selects the best macthing serialization depending on value type.
     *
     * @param nameObjectValuePair
     * @return
     */
    private Object getValueOfNameObjectValuePair(final NameObjectValuePair nameObjectValuePair) {
        if (nameObjectValuePair != null && nameObjectValuePair.getValue() != null) {

            if (TraceeContextLogAnnotationUtilities.getAnnotationFromType(nameObjectValuePair.getValue()) != null) {
                return nameObjectValuePair.getValue();
            } else if (!shouldBeIgnoreAtDeSerialization(nameObjectValuePair.getValue())) {
                return ReflectionToStringBuilder.reflectionToString(nameObjectValuePair.getValue(), new RecursiveReflectionToStringStyle());
            } else {
                // not null value - but type is in IGNORED_AT_DESERIALIZATION set
                return nameObjectValuePair.getValue().toString();
            }

        } else {
            return null;
        }
    }

    /**
     * Checks if the passed instance is of type {@link de.holisticon.util.tracee.contextlogger.data.subdata.NameStringValuePair}
     *
     * @param instance the instance to check
     * @return returns true if the instance is of type {@link de.holisticon.util.tracee.contextlogger.data.subdata.NameStringValuePair}, otherwise false
     */
    static boolean isNameStringValuePair(Object instance) {

        return instance != null && NameStringValuePair.class.isInstance(instance);

    }

    /**
     * Checks if the passed instance is of type {@link de.holisticon.util.tracee.contextlogger.data.subdata.NameStringValuePair}
     *
     * @param instance the instance to check
     * @return returns true if the instance is of type {@link de.holisticon.util.tracee.contextlogger.data.subdata.NameStringValuePair}, otherwise false
     */
    static boolean isNameObjectValuePair(Object instance) {

        return instance != null && NameObjectValuePair.class.isInstance(instance);

    }

    /**
     * Checks whether the passed instance has to be ignore at the deserialization.
     *
     * @param instance the instance to check
     * @return true if passed instance is null or type of passed instance is in IGNORED_AT_DESERIALIZATION set.
     */
    static boolean shouldBeIgnoreAtDeSerialization(final Object instance) {
        return instance == null || IGNORED_AT_DESERIALIZATION.contains(instance.getClass());
    }


}
