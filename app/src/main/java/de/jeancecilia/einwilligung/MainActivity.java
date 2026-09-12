package de.jeancecilia.einwilligung;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText practitionerInput;
    private EditText patientInput;
    private CheckBox oralInfoCheck;
    private CheckBox consentCheck;
    private SignatureView signatureView;
    private TextView statusText;
    private File lastSavedFile;

    private static final String CONSENT_TEXT =
            "Ich willige freiwillig in die mit mir besprochene Behandlung im Rahmen der " +
            "Heilpraktikererlaubnis, beschränkt auf das Gebiet der Psychotherapie, ein. " +
            "Ich wurde vor Beginn in verständlicher Form über Art, Ablauf, Ziel, mögliche " +
            "Belastungen und Risiken, Erfolgsaussichten sowie – soweit relevant – über " +
            "Behandlungsalternativen informiert und hatte Gelegenheit, Fragen zu stellen. " +
            "Mir ist bekannt, dass kein bestimmter Behandlungserfolg garantiert werden kann. " +
            "Die Einwilligung kann jederzeit mit Wirkung für die Zukunft widerrufen werden.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Einwilligung zur Behandlung", 24, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Kurzformular vor Beginn der Anamnese / Behandlung", 14, Typeface.NORMAL);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        root.addView(label("Behandler/in"));
        practitionerInput = new EditText(this);
        practitionerInput.setText("Jean-Maurice Cecilia-Menzel – Heilpraktiker, beschränkt auf das Gebiet der Psychotherapie");
        practitionerInput.setTextSize(15);
        practitionerInput.setSingleLine(false);
        practitionerInput.setMinLines(2);
        practitionerInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        practitionerInput.setBackgroundColor(Color.WHITE);
        root.addView(practitionerInput, matchWrap());
        addSpacer(root, 14);

        root.addView(label("Name der Patientin / des Patienten"));
        patientInput = new EditText(this);
        patientInput.setHint("Vor- und Nachname");
        patientInput.setTextSize(18);
        patientInput.setSingleLine(true);
        patientInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        patientInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        patientInput.setBackgroundColor(Color.WHITE);
        root.addView(patientInput, matchWrap());
        addSpacer(root, 16);

        TextView date = text("Datum: " + new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(new Date()), 15, Typeface.BOLD);
        root.addView(date);
        addSpacer(root, 12);

        TextView consentBox = text(CONSENT_TEXT, 15, Typeface.NORMAL);
        consentBox.setLineSpacing(0, 1.15f);
        consentBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        consentBox.setBackgroundColor(Color.WHITE);
        root.addView(consentBox, matchWrap());
        addSpacer(root, 14);

        oralInfoCheck = new CheckBox(this);
        oralInfoCheck.setText("Ich bestätige, dass die Behandlung und meine Fragen zuvor persönlich bzw. mündlich besprochen wurden.");
        oralInfoCheck.setTextSize(15);
        root.addView(oralInfoCheck, matchWrap());

        consentCheck = new CheckBox(this);
        consentCheck.setText("Ich habe den Text verstanden und willige freiwillig in die besprochene Behandlung ein.");
        consentCheck.setTextSize(15);
        root.addView(consentCheck, matchWrap());
        addSpacer(root, 18);

        root.addView(label("Unterschrift"));
        TextView hint = text("Bitte mit dem Finger im Feld unterschreiben.", 13, Typeface.NORMAL);
        hint.setTextColor(Color.DKGRAY);
        hint.setPadding(0, 0, 0, dp(7));
        root.addView(hint);

        signatureView = new SignatureView(this);
        LinearLayout.LayoutParams sigParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(210));
        root.addView(signatureView, sigParams);

        Button clearButton = new Button(this);
        clearButton.setText("Unterschrift löschen");
        clearButton.setOnClickListener(v -> signatureView.clear());
        root.addView(clearButton, matchWrap());
        addSpacer(root, 14);

        Button saveButton = new Button(this);
        saveButton.setText("Einwilligung als PDF speichern");
        saveButton.setTextSize(17);
        saveButton.setMinHeight(dp(54));
        saveButton.setOnClickListener(v -> saveConsent());
        root.addView(saveButton, matchWrap());

        Button shareLastButton = new Button(this);
        shareLastButton.setText("Letzte PDF teilen");
        shareLastButton.setOnClickListener(v -> {
            if (lastSavedFile != null && lastSavedFile.exists()) {
                sharePdf(lastSavedFile);
            } else {
                Toast.makeText(this, "Noch keine PDF in dieser Sitzung gespeichert.", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(shareLastButton, matchWrap());

        statusText = text("Die PDFs werden nur lokal im privaten App-Speicher abgelegt.", 12, Typeface.NORMAL);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(0, dp(10), 0, 0);
        root.addView(statusText);

        return scrollView;
    }

    private void saveConsent() {
        String patient = patientInput.getText().toString().trim();
        String practitioner = practitionerInput.getText().toString().trim();

        if (practitioner.isEmpty()) {
            practitionerInput.setError("Bitte Behandler/in eintragen.");
            practitionerInput.requestFocus();
            return;
        }
        if (patient.isEmpty()) {
            patientInput.setError("Bitte Namen eintragen.");
            patientInput.requestFocus();
            return;
        }
        if (!oralInfoCheck.isChecked() || !consentCheck.isChecked()) {
            Toast.makeText(this, "Bitte beide Bestätigungen anhaken.", Toast.LENGTH_LONG).show();
            return;
        }
        if (signatureView.isEmpty()) {
            Toast.makeText(this, "Bitte zuerst unterschreiben.", Toast.LENGTH_LONG).show();
            return;
        }

        File dir = new File(getFilesDir(), "consents");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(this, "Speicherordner konnte nicht erstellt werden.", Toast.LENGTH_LONG).show();
            return;
        }

        String safeName = patient.replaceAll("[^A-Za-z0-9ÄÖÜäöüß_-]+", "_");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.GERMANY).format(new Date());
        File file = new File(dir, "Einwilligung_" + safeName + "_" + timestamp + ".pdf");

        try {
            createPdf(file, patient, practitioner);
            lastSavedFile = file;
            statusText.setText("Gespeichert: " + file.getName());

            new AlertDialog.Builder(this)
                    .setTitle("Einwilligung gespeichert")
                    .setMessage("Die PDF wurde lokal gespeichert. Bitte sichern Sie sie zusätzlich in der Patientenakte. Möchten Sie die PDF jetzt teilen bzw. exportieren?")
                    .setPositiveButton("PDF teilen", (dialog, which) -> sharePdf(file))
                    .setNeutralButton("Neue Einwilligung", (dialog, which) -> resetForm())
                    .setNegativeButton("Schließen", null)
                    .show();
        } catch (IOException e) {
            Toast.makeText(this, "PDF konnte nicht gespeichert werden: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createPdf(File file, String patient, String practitioner) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(20);
        canvas.drawText("Einwilligung zur Behandlung", 42, 55, paint);

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10.5f);
        int y = 82;
        y = drawWrappedText(canvas, paint, "Behandler/in: " + practitioner, 42, y, 510, 14);
        y += 5;
        y = drawWrappedText(canvas, paint, "Patient/in: " + patient, 42, y, 510, 14);
        y += 5;
        String dateTime = new SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY).format(new Date());
        y = drawWrappedText(canvas, paint, "Datum/Uhrzeit: " + dateTime, 42, y, 510, 14);

        y += 14;
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(12);
        canvas.drawText("Einwilligungserklärung", 42, y, paint);
        y += 20;

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10.5f);
        y = drawWrappedText(canvas, paint, CONSENT_TEXT, 42, y, 510, 15);
        y += 12;
        y = drawWrappedText(canvas, paint,
                "[X] Die Behandlung und offene Fragen wurden zuvor persönlich bzw. mündlich besprochen.",
                42, y, 510, 15);
        y += 4;
        y = drawWrappedText(canvas, paint,
                "[X] Ich habe den Text verstanden und willige freiwillig in die besprochene Behandlung ein.",
                42, y, 510, 15);

        y += 22;
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(11);
        canvas.drawText("Unterschrift Patient/in", 42, y, paint);
        y += 10;

        RectF sigBox = new RectF(42, y, 553, y + 145);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.GRAY);
        canvas.drawRect(sigBox, border);
        signatureView.drawSignature(canvas, sigBox);
        y += 162;

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(9.5f);
        y = drawWrappedText(canvas, paint,
                "Hinweis: Diese schriftliche Bestätigung dokumentiert die Einwilligung. Sie ersetzt nicht die erforderliche individuelle Aufklärung und das Behandlungsgespräch.",
                42, y, 510, 13);
        y += 7;
        drawWrappedText(canvas, paint,
                "Die Einwilligung kann jederzeit mit Wirkung für die Zukunft widerrufen werden.",
                42, y, 510, 13);

        document.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }
    }

    private int drawWrappedText(Canvas canvas, Paint paint, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] paragraphs = text.split("\\n");
        int currentY = y;
        for (String paragraph : paragraphs) {
            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                    canvas.drawText(line.toString(), x, currentY, paint);
                    currentY += lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (line.length() > 0) {
                canvas.drawText(line.toString(), x, currentY, paint);
                currentY += lineHeight;
            }
        }
        return currentY;
    }

    private void sharePdf(File file) {
        Uri uri = Uri.parse("content://de.jeancecilia.einwilligung.files/" + Uri.encode(file.getName()));
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Einwilligung teilen / exportieren"));
    }

    private void resetForm() {
        patientInput.setText("");
        oralInfoCheck.setChecked(false);
        consentCheck.setChecked(false);
        signatureView.clear();
        statusText.setText("Bereit für die nächste Einwilligung.");
        patientInput.requestFocus();
    }

    private TextView label(String value) {
        TextView v = text(value, 14, Typeface.BOLD);
        v.setPadding(0, 0, 0, dp(5));
        return v;
    }

    private TextView text(String value, float sizeSp, int style) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sizeSp);
        v.setTextColor(Color.rgb(25, 25, 25));
        v.setTypeface(Typeface.DEFAULT, style);
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void addSpacer(LinearLayout root, int heightDp) {
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public static class SignatureView extends View {
        private final Paint signaturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private boolean empty = true;

        public SignatureView(android.content.Context context) {
            super(context);
            setBackgroundColor(Color.WHITE);
            signaturePaint.setColor(Color.BLACK);
            signaturePaint.setStrokeWidth(4f);
            signaturePaint.setStyle(Paint.Style.STROKE);
            signaturePaint.setStrokeCap(Paint.Cap.ROUND);
            signaturePaint.setStrokeJoin(Paint.Join.ROUND);
            borderPaint.setColor(Color.rgb(170, 170, 170));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, borderPaint);
            canvas.drawPath(path, signaturePaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    path.moveTo(x, y);
                    empty = false;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    invalidate();
                    return true;
                default:
                    return false;
            }
        }

        public void clear() {
            path.reset();
            empty = true;
            invalidate();
        }

        public boolean isEmpty() {
            return empty;
        }

        public void drawSignature(Canvas canvas, RectF dest) {
            if (empty || getWidth() <= 0 || getHeight() <= 0) return;
            canvas.save();
            float scaleX = dest.width() / getWidth();
            float scaleY = dest.height() / getHeight();
            float scale = Math.min(scaleX, scaleY);
            float offsetX = dest.left + (dest.width() - getWidth() * scale) / 2f;
            float offsetY = dest.top + (dest.height() - getHeight() * scale) / 2f;
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            Paint pdfPaint = new Paint(signaturePaint);
            pdfPaint.setStrokeWidth(3f / Math.max(scale, 0.01f));
            canvas.drawPath(path, pdfPaint);
            canvas.restore();
        }
    }
}
