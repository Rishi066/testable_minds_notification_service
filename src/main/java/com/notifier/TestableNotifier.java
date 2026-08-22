package com.notifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class TestableNotifier
{
    public static final String COOKIE_FILE = "cookies.json";

    public static Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
        CookieManager cookieManager = loadCookies();
    }

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
