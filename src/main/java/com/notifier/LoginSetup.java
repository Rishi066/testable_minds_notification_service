package com.notifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class LoginSetup
{
    private static final String COOKIE_FILE = "cookies.json";

    public static void main(String[] args) throws IOException
    {
        try(Playwright playwright = Playwright.create())
        {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate("https://minds.testable.org/login");

            System.out.println("\n\nA browser window has been opened");

            System.out.println("\n\nLogin to your google account (sign in with google) in the opened window");
            System.out.println("\n\nOnce you have logged in, press ENTER");

            Scanner sc = new Scanner(System.in);
            sc.nextLine();

            List<Cookie> cookies = context.cookies();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try(FileWriter writer = new FileWriter(COOKIE_FILE))
            {
                gson.toJson(cookies,writer);
            }

            System.out.println("Saved " + cookies.size() + " cookies to " + COOKIE_FILE);
            browser.close();
        }
    }
}
