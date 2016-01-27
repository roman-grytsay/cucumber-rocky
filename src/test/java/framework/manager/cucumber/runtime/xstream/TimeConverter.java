package test.java.framework.manager.cucumber.runtime.xstream;

import test.java.framework.manager.cucumber.runtime.ParameterInfo;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

abstract class TimeConverter<T> extends ConverterWithFormat<T> {
    private final List<DateFormat> formats = new ArrayList<>();
    private String format;

    TimeConverter(Locale locale, Class[] convertibleTypes) {
        super(convertibleTypes);

        addFormat(DateFormat.SHORT, locale);
        addFormat(DateFormat.MEDIUM, locale);
        addFormat(DateFormat.LONG, locale);
        addFormat(DateFormat.FULL, locale);
    }

    void addFormat(int style, Locale locale) {
        add(DateFormat.getDateInstance(style, locale));
    }

    void add(DateFormat dateFormat) {
        dateFormat.setLenient(false);
        formats.add(dateFormat);
    }

    public List<? extends Format> getFormats() {
        return format == null ? formats : Collections.singletonList(getOnlyFormat());
    }

    private Format getOnlyFormat() {
        DateFormat dateFormat = new SimpleDateFormat(format, getLocale());
        dateFormat.setLenient(false);

        return dateFormat;
    }

    @Override
    public String toString(Object obj) {
        if (obj instanceof Calendar) {
            obj = ((Calendar) obj).getTime();
        }
        return super.toString(obj);
    }

    @Override
    public void setParameterInfoAndLocale(ParameterInfo parameterInfo, Locale locale) {
        super.setParameterInfoAndLocale(parameterInfo, locale);

        if (parameterInfo.getFormat() != null) {
            format = parameterInfo.getFormat();
        }
    }

    public void removeOnlyFormat() {
        format = null;
    }

    public static List<Class> getTimeClasses() {
        List<Class> classes = new ArrayList<>();
        classes.add(Date.class);
        classes.add(Calendar.class);
        return classes;
    }
}
