package com.github.curiousoddman.curious_tunes.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ListIterator;

@RequiredArgsConstructor
public class ListIteratorWrapper<E> implements ListIterator<E> {
    private final ListIterator<E> listIterator;
    @Getter
    private E last;

    @Override
    public boolean hasNext() {
        return listIterator.hasNext();
    }

    @Override
    public E next() {
        last = listIterator.next();
        return last;
    }

    @Override
    public boolean hasPrevious() {
        return listIterator.hasPrevious();
    }

    @Override
    public E previous() {
        last = listIterator.previous();
        return last;
    }

    @Override
    public int nextIndex() {
        return listIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        return listIterator.previousIndex();
    }

    @Override
    public void remove() {
        listIterator.remove();
    }

    @Override
    public void set(E e) {
        listIterator.set(e);
    }

    @Override
    public void add(E e) {
        listIterator.add(e);
    }
}
