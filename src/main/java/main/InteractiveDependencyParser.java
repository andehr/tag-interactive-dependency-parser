package main;

// Visualising NLP problems

import com.google.common.collect.Sets;
import com.googlecode.whatswrong.NLPCanvas;
import com.googlecode.whatswrong.NLPInstance;
import com.googlecode.whatswrong.NLPInstanceFilter;
import com.googlecode.whatswrong.Token;
import com.googlecode.whatswrong.WhatsWrongWithMyNLP;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.TweetTagConverter;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static uk.ac.susx.tag.classificationframework.Util.buildParsingPipeline;
/*
 * #%L
 * InteractiveDependencyParser.java - InteractiveDependencyParser - University of Sussex - 2,013
 * %%
 * Copyright (C) 2013 - 2014 University of Sussex
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Main method sets up an interactive parsing environment.
 *
 * But there are the "visualDependencyTree()" methods which can be called with a parsed
 * document from the classification framework, which will simply open a window and visualise
 * the tree.
 *
 * User: Andrew D. Robertson
 * Date: 15/01/2014
 * Time: 10:39
 */
public class InteractiveDependencyParser {

    private static Pattern whitespace = Pattern.compile("\\s+");

    private static Set<String> pos = Sets.newHashSet("!","$","&",",","A","D","G","N","O","P","R","T","V","X","^");

    private static Set<String> deprels = Sets.newHashSet(
            "abbrev", "acomp", "advcl", "advmod", "amod", "appos", "attr", "aux", "auxpass",
            "cc", "ccomp", "complm", "conj", "cop", "csubj", "csubjpass", "dep", "det", "dobj",
            "expl", "infmod", "iobj", "mark", "mwe", "neg", "nn", "npadvmod", "nsubj", "nsubjpass",
            "num", "number", "parataxis", "partmod", "pcomp", "pobj", "poss", "possessive", "preconj",
            "predet", "prep", "prt", "punct", "purpcl", "quantmod", "rcmod", "rel", "root", "tmod", "xcomp");

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        interactiveParserTest();
    }

    /**
     * Visualise the dependency tree assigned to a document.
     */
    public static JFrame visualiseDependencyTree(Document document, String title){
        return visualiseDependencyTree((List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens"), title);
    }

    public static JFrame visualiseDependencyTree(List<TweetTagConverter.Token> tokens, String title){
        NLPInstance instance = new NLPInstance();
        instance.addToken().addProperty("form", "<ROOT>"); // Add root token
        for (int i = 0; i < tokens.size(); i++) instance.addToken(); // Seems like we have to add all the tokens first...
        for (int i = 0; i < tokens.size(); i++) {
            TweetTagConverter.Token token = tokens.get(i);
            Token t = instance.getToken(i+1);
            t.addProperty("form", token.form);
            t.addProperty("pos", token.pos);
            instance.addDependency(token.head, i + 1, token.deprel, "syntactic");
        }
        NLPCanvas canvas = new WhatsWrongWithMyNLP().getNlpCanvas();
        canvas.setNLPInstance(instance);
        canvas.setFilter(new NLPInstanceFilter() { public NLPInstance filter(NLPInstance original) { return original; }}); // Get around annoying WhatsWrongWithMyNLP bug number 1
        canvas.updateNLPGraphics();
        JFrame j = new JFrame(title.isEmpty()? "Dependency Graph" : "Dependency Graph: "+title);
        canvas.setTextArea(new JTextArea()); // Get around annoying WhatsWrongWithMyNLP bug number 2
        j.getContentPane().add(canvas, BorderLayout.CENTER);
        j.pack(); j.setResizable(false); j.setVisible(true);
        return j;
    }

    /**
     * Run an interactive session in which the user can input a sentence and view the
     * dependency tree and plain text CoNLL-style version of it.
     */
    public static void interactiveParserTest() throws IOException, ClassNotFoundException {
        System.out.print("Loading parsing framework... ");
        final FeatureExtractionPipeline pipeline = buildParsingPipeline(true, true);
        System.out.println("Done.");

        final JTextField textField = new JTextField("This is a very exciting example sentence thatll blow your mind and soul, leaving you with no hope of recovery.");
        final NLPCanvas canvas = new WhatsWrongWithMyNLP().getNlpCanvas();
        final JTextPane plainText = new JTextPane(); //plainText.setEditable(false);

        final JTextField trainingFilePath = new JTextField("[output file path]");
        trainingFilePath.setColumns(10);
        final JLabel trainingFileStatus = new JLabel("[status]");

        canvas.setFilter(new NLPInstanceFilter() { public NLPInstance filter(NLPInstance original) { return original; }}); // Get around annoying WhatsWrongWithMyNLP bug 1
        canvas.setTextArea(new JTextArea()); // Get around annoying WhatsWrongWithMyNLP bug 2

        // Tag and parse example sentence.
        List<TweetTagConverter.Token> tokens = (List<TweetTagConverter.Token>)pipeline.processDocumentWithoutCache(new Instance("", textField.getText(), "")).getAttribute("ExpandedTokens");
        // Produce tree
        canvas.setNLPInstance(getNLPInstance(tokens)); canvas.updateNLPGraphics();
        // Produce plain text
        fillTextPane(tokens, plainText);

        // Create main window.
        final JFrame mainFrame = new JFrame("Interactive Dependency Parser");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Create tree display panel
        final JPanel treePanel = new JPanel();
        treePanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 10, 0, 10), BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(184,207,229)), "Tree")));
        treePanel.setBackground(Color.white);
        final JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.add(canvas); centeringPanel.setBackground(Color.white);
        final JScrollPane treeScrollPane = new JScrollPane(centeringPanel);
        treeScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        treeScrollPane.setPreferredSize(new Dimension(485, 200));
        treePanel.add(treeScrollPane);

        // Create plain text display panel
        final JPanel plainPanel = new JPanel(new BorderLayout());
        plainPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 10, 5, 10), BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(184, 207, 229)), "Plain")));
        plainPanel.setBackground(Color.white);
        JPanel noWrapPanel = new JPanel(new BorderLayout());

        JPanel trainingFilePanel = new JPanel();
        trainingFilePanel.setBackground(Color.white);

        trainingFilePanel.add(trainingFileStatus);
        trainingFilePanel.add(trainingFilePath);

        trainingFilePath.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { statusUpdate(); }
            public void removeUpdate(DocumentEvent e) { statusUpdate(); }
            public void changedUpdate(DocumentEvent e) { statusUpdate(); }
            public void statusUpdate(){
                String path = trainingFilePath.getText().trim();
                if (path.equals("")){
                    trainingFileStatus.setText("No file found.");
                } else {
                    File trainingFile = new File(path);
                    if (trainingFile.exists()) {
                        if (trainingFile.isDirectory()){
                            trainingFileStatus.setText("No file found.");
                        } else {
                            trainingFileStatus.setText("File found.");
                        }
                    } else {
                        trainingFileStatus.setText("Create new file?");
                    }
                }
            }
        });


        final JButton append = new JButton("Append");
        trainingFilePath.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {  append.doClick(); }
        });

        append.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String path = trainingFilePath.getText().trim();
                if (path.equals("")){
                    updateStatus(trainingFileStatus, "No file found.");
                } else {
                    String parsingOuput = plainText.getText().trim();
                    if (parsingOuput.equals("")){
                        updateStatus(trainingFileStatus, "No parsing output detected.");
                    } else {
                        File trainingFile = new File(path).getAbsoluteFile();
                        if (!trainingFile.exists()) {
                            try {
                                trainingFile.getParentFile().mkdirs();
                                trainingFile.createNewFile();
                            } catch (IOException e1) {
                                updateStatus(trainingFileStatus, "Exception! Writing probably didn't happen.");
                                e1.printStackTrace();
                                return;
                            }
                        }
                        if (trainingFile.isDirectory()) {
                            updateStatus(trainingFileStatus, "Specify a FILE.");
                        } else {
                            String[] lines = parsingOuput.split(System.lineSeparator());
                            int numAttributes = whitespace.split(lines[0]).length;
                            for (int i = 0; i < lines.length; i++) {
                                String line = lines[i];
                                String[] items = whitespace.split(line);
                                if (numAttributes != items.length) {
                                    updateStatus(trainingFileStatus,"Failed. Inconsistent token attributes." ); return;
                                }
                                if (Integer.parseInt(items[0]) != i+1) {
                                    updateStatus(trainingFileStatus, "Failed. Inconsistent token IDs."); return;
                                }
                                if (!pos.contains(items[2])) {
                                    updateStatus(trainingFileStatus, "Failed. Unrecognised PoS tag."); return;
                                }
                                if (!deprels.contains(items[4])){
                                    updateStatus(trainingFileStatus, "Failed. Unrecognised dependency relation."); return;
                                }
                            }
                            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainingFile, true), "UTF-8"))){
                                bw.write(parsingOuput);
                                bw.write(System.lineSeparator());
                                bw.write(System.lineSeparator());
                                updateStatus(trainingFileStatus, "Success!");

                            } catch (IOException e1) {
                                e1.printStackTrace();
                                updateStatus(trainingFileStatus, "Exception! Writing probably didn't happen.");
                            }
                        }
                    }
                }
            }
        });
        trainingFilePanel.add(append);

        noWrapPanel.add(plainText);

        final JScrollPane plainScrollPane = new JScrollPane(noWrapPanel);


        plainScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        plainScrollPane.setPreferredSize(new Dimension(485, 400));
        plainPanel.add(plainScrollPane, BorderLayout.WEST);

        plainPanel.add(trainingFilePanel, BorderLayout.SOUTH);


        // Create text input panel
        final JPanel textFieldPanel = new JPanel(new GridLayout(1,1));
        textFieldPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
        textFieldPanel.setBackground(new Color(222, 239, 255));
        textFieldPanel.add(textField, 0);

        // Assign panels to the content pane
        Container content = mainFrame.getContentPane();
        content.add(treePanel, BorderLayout.NORTH);
        content.add(plainPanel, BorderLayout.CENTER);
        content.add(textFieldPanel, BorderLayout.SOUTH);

        // Organise and display
        mainFrame.pack(); mainFrame.setResizable(true);

        mainFrame.setSize(new Dimension(520, 750));

        mainFrame.setVisible(true);

        // To be executed when new text is input followed by a press of the enter key
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Parse new text and re-create displays
                textField.setText(textField.getText().replaceAll("\\p{C}", "").trim()); // Strip non-printable control characters
                List<TweetTagConverter.Token> tokens = (List<TweetTagConverter.Token>) pipeline.processDocumentWithoutCache(new Instance("", textField.getText(), "")).getAttribute("ExpandedTokens");
                canvas.setNLPInstance(getNLPInstance(tokens));
                canvas.updateNLPGraphics();
                fillTextPane(tokens, plainText);
                treePanel.revalidate();
            }
        });

        // To be executed when new text is put in the "Plain" pane followed by a "SHIFT+ENTER" key press.
        // Display the tree described in the text pane.
        Action shiftEnter = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setNLPInstance(getNLPInstance(plainText.getText(), false));
                canvas.updateNLPGraphics(); treePanel.revalidate();
            }
        };
        plainText.getInputMap().put(KeyStroke.getKeyStroke("shift ENTER"), "shiftEnter");
        plainText.getActionMap().put("shiftEnter", shiftEnter);

        // To be executed when new text is put in the "Plain" pane followed by a "ALT+ENTER" key press.
        // Will first remove any "_" columns from the data (allows more compatibility with other CoNLL formats)
        // Then display the tree in the text pane.
        Action altEnter = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setNLPInstance(getNLPInstance(plainText.getText(), true));
                canvas.updateNLPGraphics(); treePanel.revalidate();
            }
        };
        plainText.getInputMap().put(KeyStroke.getKeyStroke("alt ENTER"), "altEnter");
        plainText.getActionMap().put("altEnter", altEnter);

        // Add mouse click listeners which hide inner components when clicked.
        treePanel.addMouseListener(new HidingMouseListener(treeScrollPane, mainFrame));
        plainPanel.addMouseListener(new HidingMouseListener(plainScrollPane, mainFrame));

        // Add listener for resizing capabilities.
        mainFrame.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                treeScrollPane.setPreferredSize(new Dimension(treePanel.getWidth() - 40, Math.min(200, mainFrame.getHeight()-100)));
                plainScrollPane.setPreferredSize(new Dimension(plainPanel.getWidth() - 40, 200));
            }
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
            public void componentHidden(ComponentEvent e) {}
        });
    }

    /**
     * Convert a parsed sentence into the data structure required by the
     * visualisation library.
     */
    private static NLPInstance getNLPInstance(List<TweetTagConverter.Token> tokens){
        NLPInstance instance = new NLPInstance();
        instance.addToken().addProperty("form", "<ROOT>"); // Add root token
        for (int i = 0; i < tokens.size(); i++) instance.addToken(); // Seems like we have to add all the tokens first...
        for (int i = 0; i < tokens.size(); i++) {
            TweetTagConverter.Token token = tokens.get(i);
            Token t = instance.getToken(i+1);
            t.addProperty("form", token.form);
            t.addProperty("pos", token.pos);
            instance.addDependency(token.head, i + 1, token.deprel, "syntactic");
        }
        return instance;
    }

    /**
     * Convert marked up text into a NLPInstance
     */
    private static NLPInstance getNLPInstance(String text, boolean removeEmptyFields){
        text = text.trim();
        if (text.isEmpty()) return new NLPInstance();
        String[] lines = text.split(System.lineSeparator());
        List<TweetTagConverter.Token> tokens = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String[] items;
            if (removeEmptyFields) {
                items = lines[i].replaceAll("_", "").replaceAll("\t\t+", "\t").trim().split("\\s+");
            } else {
                items = lines[i].trim().split("\\s+");
            }
            TweetTagConverter.Token token = new TweetTagConverter.Token(Integer.parseInt(items[0]), items[1], items[2]);
            token.head = Integer.parseInt(items[3]);
            token.deprel = items[4];
            tokens.add(token);
        }
        return getNLPInstance(tokens);
    }

    /**
     * Given a list of parsed tokens, fill a JTextPane with a pseudo-CoNLL representation of the tokens.
     */
    private static void fillTextPane(List<TweetTagConverter.Token> tokens, JTextPane pane){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            TweetTagConverter.Token token = tokens.get(i);
            sb.append(i+1); sb.append("\t");          // ID
            sb.append(token.form); sb.append("\t");   // Form
            sb.append(token.pos); sb.append("\t");    // POS
            sb.append(token.head); sb.append("\t");   // head
            sb.append(token.deprel); sb.append("\t"); // Deprel
            sb.append(System.lineSeparator());
        }
        pane.setText(sb.toString());
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet left = new SimpleAttributeSet();
        StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
        StyleConstants.setForeground(left, new Color(124, 124, 124));
        doc.setParagraphAttributes(0, doc.getLength(), left, false);
    }

    /**
     * A MouseListener that on mouse release hides a sub-component of the container to which it is added.
     */
    private static class HidingMouseListener implements MouseListener {

        private boolean hidden = false;
        private Component toBeHidden;
        private JFrame applicationFrame;

        public HidingMouseListener(Component toBeHidden, JFrame applicationFrame){
            this.toBeHidden = toBeHidden;
            this.applicationFrame = applicationFrame;
        }

        public void mouseReleased(MouseEvent e) {
            Container source = (Container)e.getSource();
            if (hidden){
                source.add(toBeHidden);
                hidden = false;
            } else {
                source.remove(toBeHidden);
                applicationFrame.setSize(new Dimension(applicationFrame.getWidth(), applicationFrame.getHeight() - toBeHidden.getHeight()));
                hidden = true;
            }
            int heightBeforeRepaint = applicationFrame.getHeight();
            applicationFrame.revalidate();
            applicationFrame.repaint();
            if (!hidden) applicationFrame.setSize(new Dimension(applicationFrame.getWidth(), heightBeforeRepaint + Math.max(200-toBeHidden.getHeight(),0)));
        }
        public void mouseClicked(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
    }

    private static void updateStatus(JLabel status, String update){
        status.setText(update);
        ColorTip tip = new ColorTip(0, 1, status, new Color(222,239,254));
        tip.start();
    }
}
