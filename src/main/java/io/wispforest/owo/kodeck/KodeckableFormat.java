package io.wispforest.owo.kodeck;

public interface KodeckableFormat<T> extends Format<T> {

    public FormatKodeck<T> getFormatKodeck();

}
