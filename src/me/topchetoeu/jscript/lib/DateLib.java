package me.topchetoeu.jscript.lib;

import java.util.Calendar;
import java.util.TimeZone;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.interop.Native;

public class DateLib {
    private Calendar normal;
    private Calendar utc;

    private void updateUTC() {
        if (utc == null || normal == null) return;
        utc.setTimeInMillis(normal.getTimeInMillis());
    }
    private void updateNormal() {
        if (utc == null || normal == null) return;
        normal.setTimeInMillis(utc.getTimeInMillis());
    }
    private void invalidate() {
        normal = utc = null;
    }

    @Native
    public static double now() {
        return new DateLib().getTime();
    }

    @Native
    public double getYear() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.YEAR) - 1900;
    }
    @Native
    public double setYear(Context ctx, double real) throws InterruptedException {
        if (real >= 0 && real <= 99) real = real + 1900;
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.YEAR, (int)real);
        updateUTC();
        return getTime();
    }

    @Native
    public double getFullYear() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.YEAR);
    }
    @Native
    public double getMonth() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MONTH);
    }
    @Native
    public double getDate() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.DAY_OF_MONTH);
    }
    @Native
    public double getDay() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.DAY_OF_WEEK);
    }
    @Native
    public double getHours() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.HOUR_OF_DAY);
    }
    @Native
    public double getMinutes() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MINUTE);
    }
    @Native
    public double getSeconds() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.SECOND);
    }
    @Native
    public double getMilliseconds() {
        if (normal == null) return Double.NaN;
        return normal.get(Calendar.MILLISECOND);
    }

    @Native
    public double getUTCFullYear() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.YEAR);
    }
    @Native
    public double getUTCMonth() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MONTH);
    }
    @Native
    public double getUTCDate() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.DAY_OF_MONTH);
    }
    @Native
    public double getUTCDay() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.DAY_OF_WEEK);
    }
    @Native
    public double getUTCHours() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.HOUR_OF_DAY);
    }
    @Native
    public double getUTCMinutes() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MINUTE);
    }
    @Native
    public double getUTCSeconds() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.SECOND);
    }
    @Native
    public double getUTCMilliseconds() {
        if (utc == null) return Double.NaN;
        return utc.get(Calendar.MILLISECOND);
    }

    @Native
    public double setFullYear(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.YEAR, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setMonth(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MONTH, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setDate(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.DAY_OF_MONTH, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setDay(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.DAY_OF_WEEK, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setHours(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.HOUR_OF_DAY, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setMinutes(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MINUTE, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setSeconds(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.SECOND, (int)real);
        updateUTC();
        return getTime();
    }
    @Native
    public double setMilliseconds(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else normal.set(Calendar.MILLISECOND, (int)real);
        updateUTC();
        return getTime();
    }

    @Native
    public double setUTCFullYear(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.YEAR, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCMonth(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MONTH, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCDate(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.DAY_OF_MONTH, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCDay(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.DAY_OF_WEEK, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCHours(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.HOUR_OF_DAY, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCMinutes(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MINUTE, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCSeconds(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.SECOND, (int)real);
        updateNormal();
        return getTime();
    }
    @Native
    public double setUTCMilliseconds(Context ctx, double real) throws InterruptedException {
        if (Double.isNaN(real)) invalidate();
        else utc.set(Calendar.MILLISECOND, (int)real);
        updateNormal();
        return getTime();
    }

    @Native
    public double getTime() {
        if (utc == null) return Double.NaN;
        return utc.getTimeInMillis();
    }
    @Native
    public double getTimezoneOffset() {
        if (normal == null) return Double.NaN;
        return normal.getTimeZone().getRawOffset() / 60000;
    }

    @Native
    public double valueOf() {
        if (normal == null) return Double.NaN;
        else return normal.getTimeInMillis();
    }

    @Native
    public String toString() {
        return normal.getTime().toString();
    }

    @Native
    public DateLib(long timestamp) {
        normal = Calendar.getInstance();
        utc = Calendar.getInstance();
        normal.setTimeInMillis(timestamp);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(timestamp);
    }

    @Native
    public DateLib() {
        this(new java.util.Date().getTime());
    }
}
