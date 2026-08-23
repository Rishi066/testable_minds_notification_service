package com.notifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

public class TestableNotifier
{
    public static final String COOKIE_FILE = "cookies.json";
    public static final String STATE_FILE = "studies_seen.json";
    public static final String STUDIES_URL = "https://minds.testable.org/browse";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    public static final int BASE_INTERVAL = 40;

    public static Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
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
                    System.out.printf("[%s] No new Studies. (%d currently listed)",new Date(),current.size());
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
    static void notify(String title,String message)
    {

    }

    //to parse studies from the study page
    static Map<String,String> parseStudies(String html)
    {

    }

    //to intimate human behavior
    static long humanDelaySeconds(int backoff)
    {

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
