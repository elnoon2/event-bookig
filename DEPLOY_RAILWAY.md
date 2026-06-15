# نشر المشروع على Railway (دليل سريع)

> ليه مش Vercel؟ الـ Backend مكتوب بـ Java / Spring Boot، وVercel مبيشغّلش Java.
> Railway بيبني المشروع من الـ Dockerfile وبيشغّل الـ Backend + الواجهة على لينك واحد.

تم تجهيز المشروع للنشر بالملفات دي:
- `Dockerfile` — يبني الـ jar ويقدّم الواجهة معاه.
- `.dockerignore`
- `application.properties` — البورت بقى `${PORT:5000}` (Railway بيحقن PORT).
- `application-prod.properties` — قاعدة البيانات بتتقري من Environment Variables.

---

## الخطوة 0 — ارفع التعديلات على GitHub
لازم الملفات الجديدة توصل للـ repo اللي Railway هيبني منه:
```
git add Dockerfile .dockerignore backend/src/main/resources/application.properties backend/src/main/resources/application-prod.properties DEPLOY_RAILWAY.md
git commit -m "Add Railway deployment (Dockerfile + prod config)"
git push
```

## الخطوة 1 — اعمل المشروع على Railway
1. railway.app → **New Project** → **Deploy from GitHub repo** → اختار الـ repo بتاع المشروع.
2. لو الـ `Dockerfile` مش في جذر الـ repo، روح **Settings → Root Directory** وحطّ المسار اللي فيه الـ Dockerfile.
3. Railway هيكتشف الـ Dockerfile ويبدأ يبني تلقائياً.

## الخطوة 2 — ضيف قاعدة بيانات MySQL
- جوه المشروع: **New → Database → Add MySQL**.

## الخطوة 3 — اظبط الـ Environment Variables على سيرفس التطبيق (مش الداتابيز)
افتح سيرفس التطبيق → **Variables** → ضيف:

| الاسم | القيمة |
|------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `SPRING_DATASOURCE_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `MICROSOFT_CLIENT_ID` | (من `microsoft-env.bat`) |
| `MICROSOFT_CLIENT_SECRET` | (من `microsoft-env.bat`) |
| `MICROSOFT_TENANT_ID` | `common` |
| `APP_ADMIN_EMAIL` | إيميل الأدمن |
| `APP_ADMIN_PASSWORD` | باسورد قوي للأدمن |
| `QR_SIGNING_SECRET` | أي قيمة عشوائية طويلة |

> `${{MySQL.XXX}}` ده مرجع تلقائي لمتغيرات سيرفس الـ MySQL — Railway بيملاها لوحده.

## الخطوة 4 — اعمل دومين واضبط الـ URLs اللي بتعتمد عليه
1. سيرفس التطبيق → **Settings → Networking → Generate Domain**. هيطلعلك زي:
   `https://your-app.up.railway.app`
2. ارجع للـ **Variables** وضيف (بدّل الدومين بدومينك):

| الاسم | القيمة |
|------|--------|
| `MICROSOFT_REDIRECT_URI` | `https://your-app.up.railway.app/api/auth/microsoft/callback` |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.up.railway.app` |
| `APP_SITE_BASE_URL` | `https://your-app.up.railway.app` |

3. Railway هيعيد النشر تلقائياً بعد تعديل المتغيرات.

## الخطوة 5 — مهم جداً: ضيف الـ Redirect URI في Azure
تسجيل الطلاب بالـ Microsoft بس. لازم تضيف رابط الـ callback الجديد في الـ App Registration:
1. portal.azure.com → **Microsoft Entra ID → App registrations** → افتح التطبيق.
2. **Authentication → Add a platform / Web → Redirect URIs** → ضيف:
   `https://your-app.up.railway.app/api/auth/microsoft/callback`
3. Save.

> من غير الخطوة دي، الـ Microsoft login هيفشل برسالة redirect mismatch ومحدش هيقدر يسجّل.

## الخطوة 6 — اتأكد إن كل حاجة شغّالة
- افتح `https://your-app.up.railway.app` → المفروض الصفحة تفتح.
- جرّب **Sign in with Microsoft** بحساب طالب → لازم يدخل ويقدر يحجز.
- لوحة الأدمن: `https://your-app.up.railway.app/admin.html`

---

### ملاحظة أمان
الـ client secret ظاهر حالياً في `microsoft-env.bat` (الملف متجاهَل في git فمش بيترفع — كويس). بعد الإيفنت يُفضّل تعمله rotate من Azure.
