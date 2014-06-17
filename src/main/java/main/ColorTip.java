package main;

import javax.swing.*;
import java.awt.*;

/**
 * Source code adapted from the freely available examples
 * found at: http://www.javapractices.com/home/HomeAction.do
 *
 * The website indicates that individual code snippets (like the one below)
 * may be used under the following BSD license:

 Copyright (c) 2002-2009, Hirondelle Systems
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Hirondelle Systems nor the
   names of its contributors may be used to endorse or promote products
   derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY HIRONDELLE SYSTEMS ''AS IS'' AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL HIRONDELLE SYSTEMS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 *
 * Calls user's attention to an aspect of the GUI (much like a
 * <tt>ToolTip</tt>) by changing the background color of a
 * component (typically a <tt>JLabel</tt>) for a few seconds;
 * the component will always revert to its original background color
 * after a short time has passed. This is done once, without repeating.
 *
 * <p>Example use case:
 <pre>
 //no initial delay, and show the new color for 2 seconds only
 ColorTip tip = new ColorTip(0, 2, someLabel, temporaryColor);
 tip.start();
 </pre>
 *
 * Uses a daemon thread, so this class will not prevent a program from
 * terminating. Will not lock the GUI.
 */
final class ColorTip {

    /**
     * Constructor.
     *
     * @param aInitialDelay number of seconds to wait before changing the
     * background color of <tt>aComponent</tt>, and must be in range 0..60 (inclusive).
     * @param aActivationInterval number of seconds to display <tt>aTempColor</tt>,
     * and must be in range 1..60 (inclusive).
     * @param aComponent GUI item whose background color will be changed.
     * @param aTempColor background color which <tt>aComponent</tt> will take for
     * <tt>aActivationInterval</tt> seconds.
     */
    ColorTip (
            int aInitialDelay, int aActivationInterval, JComponent aComponent, Color aTempColor
    ) {
        fInitialDelay = aInitialDelay;
        fActivationInterval = aActivationInterval;
        fComponent = aComponent;
        fTemporaryColor = aTempColor;
        fOriginalColor = aComponent.getBackground();
        fOriginalOpacity = aComponent.isOpaque();
    }

    /**
     * Temporarily change the background color of the component, without interfering with
     * the user's control of the gui, and without preventing program termination.
     *
     * <P>If the target temporary color is the same as the current background color, then
     * do nothing. (This condition occurs when two <tt>ColorTip</tt> objects are
     * altering the same item at nearly the same time, such that they "overlap".)
     */
    void start(){
        if (isSameColor()) return;
    /*
     * The use of a low-level Thread, instead of a more modern class, is unusual here.
     * It's acceptable since other tools aren't a great match for this task, which is to
     * go back and forth TWICE (wait, color-on, wait, color-off) between a worker thread
     * and the Event Dispatch Thread; that's not a good match for either SwingWorker
     * or javax.swing.Timer.
     */
        Thread thread = new Thread(new Worker());
        thread.setDaemon(true);
        thread.start();
    }

    // PRIVATE
    private final int fInitialDelay;
    private final int fActivationInterval;
    private final JComponent fComponent;
    private final Color fTemporaryColor;
    private final Color fOriginalColor;
    private final int fCONVERSION_FACTOR = 1000;

    /**
     * Stores the original value of the opaque property of fComponent.
     *
     * Changes to the background color of a component
     * take effect only if the component is in charge of drawing its background.
     * This is defined by the opaque property, which needs to be true for these
     * changes to take effect.
     *
     * <P>If fComponent is not opaque, then this property is temporarily
     * changed by this class in order to change the background color.
     */
    private final boolean fOriginalOpacity;

    /**
     * Return true only if fTemporaryColor is the same as the fOriginalColor.
     */
    private boolean isSameColor(){
        return fTemporaryColor.equals(fOriginalColor);
    }

    /**
     The sleeping done by this class is NOT done on the Event Dispatch Thread;
     that would lock the GUI.
     */
    private final class Worker implements Runnable {
        @Override public void run(){
            try {
                Thread.sleep(fCONVERSION_FACTOR * fInitialDelay);
                EventQueue.invokeLater(new ChangeColor());
                Thread.sleep(fCONVERSION_FACTOR * fActivationInterval);
                EventQueue.invokeLater(new RevertColor());
            }
            catch (InterruptedException ex) {
            }
        }
    }

    private final class ChangeColor implements Runnable {
        @Override public void run(){
            if (! fOriginalOpacity) {
                fComponent.setOpaque(true);
            }
            fComponent.setBackground(fTemporaryColor);
        }
    }

    private final class RevertColor implements Runnable {
        @Override public void run(){
            fComponent.setBackground(fOriginalColor);
            fComponent.setOpaque(fOriginalOpacity);
        }
    }
}