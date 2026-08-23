package com.notifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import java.util.List;

public class TestableNotifier
{
    public static final String COOKIE_FILE = "cookies.json";
    public static final String STATE_FILE = "studies_seen.json";
    public static final String STUDIES_URL = "https://minds.testable.org/browse";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    public static final int BASE_INTERVAL = 40;
    public static final double LONG_BREAK_CHANCE = 0.08;
    public static final int JITTER_SEC = 15;
    public static final int LONG_BREAK_MIN_SEC = 120;
    public static final int LONG_BREAK_MAX_SEC = 300;

    public static Gson gson = new Gson();
    public static Random random = new Random();

    public static void main(String[] args) throws Exception
    {
        System.setProperty("apple.awt.UIElement", "true");

        CookieManager cookieManager = loadCookies();

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        Map<String, String> seen = loadSeen();
        int backoff = BASE_INTERVAL;
        while (true) {
            try
            {
                String html = fetchStudiesPage(client);

                if(html.contains("Sign in to see current studies"))
                {
                    System.out.println("[!] Session Expired. Run LoginSetup again [!]");
                    notify("Notifier Stopped","Session Expired. Re-run LoginSetup");
                    return;
                }

                Map<String,String> current = parseStudies(html);
                Map<String,String> newOnes = new LinkedHashMap<>();
                for(Map.Entry<String,String> entry : current.entrySet())
                {
                    if(!seen.containsKey(entry.getKey()))
                    {
                        newOnes.put(entry.getKey(), entry.getValue());
                    }
                }

                if(newOnes.isEmpty())
                {
                    System.out.printf("[%s] No new Studies. (%d currently listed)\n",new Date(),current.size());
                }
                else {
                    for (String title : newOnes.values()) {
                        notify("New TM Study!", title);
                    }
                    seen.putAll(current);
                    saveSeen(seen);
                }
            }

            catch(IOException | InterruptedException e)
            {
                System.out.println("[!] Request failed: " + e.getMessage());
                backoff = Math.min(backoff*2,600);
            }
            Thread.sleep(1000L * humanDelaySeconds(backoff));
        }
    }

    //writing new seen studies into "studies_seen.json"
    static void saveSeen(Map<String,String> seen) throws IOException
    {
        try(Writer writer = new FileWriter(STATE_FILE))
        {
           gson.toJson(seen,writer);
        }
    }

    //notifier
    static void notify(String title, String message)
    {
        System.out.println("[NOTIFY] " + title + ": " + message);

        SwingUtilities.invokeLater(() -> showPopup(title, message));
    }


    private static JFrame hiddenOwner;
    private static JFrame getHiddenOwner()
    {
        if (hiddenOwner == null)
        {
            hiddenOwner = new JFrame();
            hiddenOwner.setUndecorated(true);
            hiddenOwner.setType(Window.Type.UTILITY); // hints to some window managers this isn't a "real" app window
            hiddenOwner.setSize(0, 0);
            hiddenOwner.setLocation(-10, -10); // fully off-screen just in case
            // never call setVisible(true) on this — it stays invisible forever
        }
        return hiddenOwner;
    }

    private static void showPopup(String title, String message)
    {
        JWindow popup = new JWindow(getHiddenOwner()); // <-- owned window, no taskbar entry
        popup.setAlwaysOnTop(true);
        popup.setFocusableWindowState(false); // <-- doesn't steal focus / slide over your active window

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(30, 30, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel messageLabel = new JLabel("<html><body style='width: 240px'>" + message + "</body></html>");
        messageLabel.setForeground(Color.LIGHT_GRAY);
        messageLabel.setFont(messageLabel.getFont().deriveFont(12f));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        panel.add(titleLabel);
        panel.add(messageLabel);
        popup.getContentPane().add(panel);
        popup.pack();

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int x = screen.x + screen.width - popup.getWidth() - 24;
        int y = screen.y + screen.height - popup.getHeight() - 24;
        popup.setLocation(x, y);

        popup.setVisible(true);

        Timer timer = new Timer(6000, e -> popup.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    //to parse studies from the study page
    static Map<String,String> parseStudies(String html)
    {
        Map<String,String> studies = new LinkedHashMap<>();
        Document doc = Jsoup.parse(html);

        Elements cards = doc.select("div.one-test");

        for(Element card : cards)
        {
            if(card.hasClass("one-test-example"))
            {
                continue;
            }

            Element titleEl = card.selectFirst("div.test-title");
            if(titleEl == null) continue;
            String title = titleEl.text().trim();
//            if(title.equals("Invite Friends and Earn $1!")) continue;
            if(title.isEmpty()) continue;

            Element researcherLink = card.selectFirst("a[href*=/researcher/]");
            String href = researcherLink != null ? researcherLink.attr("href") : "";

            studies.put(href + " :: " + title,title);
        }
        return studies;
    }

    //to intimate human behavior
    static int humanDelaySeconds(int backoff)
    {
        if(backoff != BASE_INTERVAL) return backoff;

        if(random.nextDouble() < LONG_BREAK_CHANCE)
        {
            int pause = LONG_BREAK_MIN_SEC + random.nextInt(LONG_BREAK_MAX_SEC - LONG_BREAK_MIN_SEC);
            System.out.println("[..] Taking a longer break (" + pause + "s");
            return pause;
        }
        int jitter = -JITTER_SEC + random.nextInt(2*JITTER_SEC + 1);
        return Math.max(10,BASE_INTERVAL + jitter);

    }

    //fetches HTML code of studies page
    static String fetchStudiesPage(HttpClient client) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create((STUDIES_URL)))
                .header("User-Agent",USER_AGENT)
                .header("Accept-Language","en-US,en;q=0.9")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200)
        {
            throw  new IOException("Unexpected status code: " + response.statusCode());
        }
        return response.body();
    }

    //loads seen studies to avoid notifying same studies
    static Map<String,String> loadSeen() throws IOException
    {
        File file = new File(STATE_FILE);
        if(!file.exists()) return new LinkedHashMap<>();

        try(Reader reader = new FileReader(file))
        {
            Type type = new TypeToken<Map<String,String>>(){}.getType();
            Map<String,String> result = gson.fromJson(reader,type);
            return result == null ? new LinkedHashMap<>() : result;
        }
    }

    //loads cookies from cookies.json into cookie manager
    static CookieManager loadCookies() throws IOException
    {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        File file = new File(COOKIE_FILE);
        if(!file.exists())
        {
            System.out.println(COOKIE_FILE + " not found." +"\nRun LoginSetup first and try again");
            System.exit(1);
        }

        try(Reader reader = new FileReader(file))
        {
            Type listType = new TypeToken<List<Map<String,Object>>>(){}.getType();
            List<Map<String,Object>> rawCookies = gson.fromJson(reader,listType);

            for(Map<String,Object> cookie : rawCookies)
            {
                String name = (String) cookie.get("name");
                String value = (String) cookie.get("value");

                HttpCookie httpCookie = new HttpCookie(name,value);
                httpCookie.setDomain("minds.testable.org");
                httpCookie.setPath("/");

                manager.getCookieStore().add(URI.create("https://minds.testable.org"),httpCookie);
            }
        }
        return manager;
    }
}
