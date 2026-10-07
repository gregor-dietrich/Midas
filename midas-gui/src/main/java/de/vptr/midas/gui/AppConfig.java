package de.vptr.midas.gui;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.Lumo;

/** Component. */
@StyleSheet("/" + Lumo.STYLESHEET)
@StyleSheet("/styles.css")
@PageTitle("Midas")
@Push
// Security pin: @vaadin/markdown pulls in dompurify, and whenever package-lock.json is regenerated
// Vaadin reseeds it from its own tested lock, resetting a lockfile-only bump. Declaring it here puts
// it in package.json, which regeneration preserves. 3.4.16 fixes GHSA-p98j-92pf-mc4p;
// devkit's check_frontend_deps.py enforces the minimum. Remove once Vaadin ships >= 3.4.16 itself.
@NpmPackage(value = "dompurify", version = "3.4.16")
// Security pin, same mechanism: vite pulls in source-map-js through postcss (build tooling only, not
// shipped to browsers). 1.2.2 fixes GHSA-68fv-2mgg-jv7q. Remove once Vaadin ships >= 1.2.2 itself.
@NpmPackage(value = "source-map-js", version = "1.2.2")
public class AppConfig implements AppShellConfigurator {
}
