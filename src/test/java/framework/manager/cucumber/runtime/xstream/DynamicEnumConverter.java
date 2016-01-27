package test.java.framework.manager.cucumber.runtime.xstream;

import cucumber.deps.com.thoughtworks.xstream.converters.SingleValueConverterWrapper;

import java.util.Locale;

/**
 * Creates an instance of needed {@link ConverterWithEnumFormat} dynamically based on required type
 */
class DynamicEnumConverter extends DynamicClassBasedSingleValueConverter {

    private final Locale locale;

    DynamicEnumConverter(Locale locale) {
        this.locale = locale;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SingleValueConverterWrapper converterForClass(Class type) {
        return new SingleValueConverterWrapperExt(new ConverterWithEnumFormat(locale, type));
    }

    @Override
    public boolean canConvert(Class type) {
        return type.isEnum();
    }
}
