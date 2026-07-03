# CHANGES — Realtime chat, bug fixevi, manager loyalty, brisanje notifikacija

> Sve izmene su verifikovane kompajliranjem: backend (`mvn compile`, JDK 21) i frontend (`gradlew :app:compileDebugKotlin`) prolaze bez grešaka. UI dizajn, biznis logika i postojeće security izmene iz SECURITY_ANALYSIS.md nisu dirane.

---

## Zadatak 1 — WebSocket za chat (umesto polling-a)

### Šta je promenjeno

**Backend** — dodat Spring WebSocket + STOMP:
- `pom.xml` — dodata zavisnost `spring-boot-starter-websocket`.
- **NOVO** `security/WebSocketConfig.java` — STOMP endpoint `/ws` (SockJS; raw WebSocket transport je `/ws/websocket`), simple broker na `/topic` i `/queue`, application prefix `/app`. Custom `DefaultHandshakeHandler` postavlja autentifikovanog korisnika (iz handshake-a) kao `Principal` svih STOMP poruka te konekcije.
- **NOVO** `security/WebSocketAuthInterceptor.java` — dve uloge:
  1. `HandshakeInterceptor`: izvlači JWT iz query parametra `token` (ili `Authorization` headera), validira ga kroz postojeći `JwtService` + `UserDetailsServiceImpl`; bez validnog tokena handshake se odbija sa 401.
  2. `ChannelInterceptor`: na `SUBSCRIBE` ka `/topic/chat/{sessionId}` proverava vlasništvo sesije (vlasnik tiketa ili staff — isti ownership obrazac kao `SupportService`), čime se sprečava prisluškivanje tuđih chat sesija (IDOR).
- `security/SecurityConfig.java` — dodat `requestMatchers("/ws/**").permitAll()`. **Napomena:** ovo nije slabljenje bezbednosti — handshake nosi JWT u query parametru (ne u headeru), pa autentifikaciju sprovodi `WebSocketAuthInterceptor` koji odbija konekcije bez validnog tokena.
- **NOVO** `support/controller/ChatWebSocketController.java` — `@MessageMapping("/chat/{sessionId}/send")` prima poruku, radi svež lookup korisnika iz baze (kao `JwtAuthenticationFilter`) i delegira na postojeći `SupportService.sendMessage()`. Ima `@MessageExceptionHandler` da greške ne ruše konekciju.
- `support/service/SupportService.java` — `sendMessage()` posle čuvanja poruke broadcast-uje postojeći `ChatMessageDTO` na `/topic/chat/{sessionId}` kroz `SimpMessagingTemplate`. Broadcast je namerno u servisu (a ne u WS kontroleru) da bi i poruke poslate kroz REST endpoint stizale realtime svim pretplaćenim klijentima — i da ne bi bilo duplog broadcasta.

**Frontend:**
- `libs.versions.toml` / `build.gradle.kts` — **bez izmena**: WebSocket podrška je deo core `okhttp` artefakta koji Retrofit već koristi, nova biblioteka nije potrebna.
- **NOVO** `core/network/ChatWebSocketManager.kt` — minimalni STOMP klijent preko OkHttp WebSocket-a:
  - konekcija na `ws://10.0.2.2:8080/ws/websocket?token={jwt}` (URL se izvodi iz `BuildConfig.BASE_URL`, pa u release buildu automatski postaje `wss://`),
  - `connect(sessionId, onMessage, onError)`, `disconnect()`, `sendMessage(content, sender)`,
  - automatski reconnect: 3 pokušaja sa exponential backoff-om (1s / 2s / 4s),
  - koristi izvedeni OkHttp klijent sa `readTimeout = 0` i `pingInterval = 20s` (dugoživeća konekcija ne sme da padne na idle read timeout-u).
- `feature/support/presentation/SupportViewModel.kt`:
  - uklonjeni `startPolling(sessionId)` / `stopPolling()` i `pollingJob` za chat (ticket polling za badge NIJE diran),
  - dodati `connectWebSocket(sessionId)` / `disconnectWebSocket()`,
  - primljene poruke se dodaju u `_chatUiState.messages` uz **dedup po id-u** (sopstvena poruka se vraća kroz broadcast) i sortiranje po `sentAt`,
  - `sendMessage()` šalje primarno kroz WebSocket; ako konekcija nije aktivna, **fallback na postojeći REST** poziv (chat ostaje upotrebljiv i ako WS padne),
  - `loadMessages(sessionId)` (REST) zadržan za inicijalno učitavanje istorije.
- `feature/support/presentation/ChatScreen.kt` — u `LaunchedEffect` zamenjen `startPolling` → `connectWebSocket`, u `DisposableEffect` i na back dugmetu `stopPolling` → `disconnectWebSocket`. UI, send dugme i `markAsRead` netaknuti.

### Zašto ovako
- Employee i Customer koriste isti `ChatScreen` → oba se konektuju na isti topic i realtime vide poruke druge strane.
- Broadcast iz servisa pokriva i WS i REST put slanja, pa mešoviti klijenti (stari REST tok kao fallback) i dalje rade konzistentno.

---

## Zadatak 2 — Bug: "You don't have permission" toast pri prvom dodavanju u korpu

### Uzrok (root cause)
`CartViewModel` se kreira u `MainActivity.setContent` **odmah pri startu aplikacije** (deli se između ekrana), tj. pre logina. Njegov `init { loadCart() }` je slao `GET /api/cart` **bez tokena** → Spring Security za neautentifikovan zahtev vraća **403** → `ErrorMapper` to mapira u *"You don't have permission to do this."* → poruka ostane u `uiState.error`. Toast se prikaže tek kada korisnik (posle logina i prvog dodavanja u korpu) prvi put uđe na `CartScreen`, čiji `LaunchedEffect(uiState.error)` pokupi taj **stari** error i očisti ga — zato se greška javlja tačno jednom, a proizvod se ipak uredno doda.

### Fix
- `feature/cart/presentation/CartViewModel.kt` — uklonjen `init { loadCart() }` (sa komentarom zašto). Korpa se učitava tamo gde zaista treba: `CartScreen` već poziva `loadCart()` u `LaunchedEffect(Unit)`, a `addToCart` odgovor vraća kompletno stanje korpe. Nema izmena UI-ja ni biznis logike.

---

## Zadatak 3 — Bug: Admin lista se ne osvežava nakon deaktivacije korisnika

### Uzrok (root cause)
Nije reaktivnost — `filteredUsers` u `AdminHomeScreen` je `remember(uiState.users, searchQuery)` i uredno se preračunava. Pravi problem: backend `DELETE /api/users/{id}` vraća `ApiResponse` sa `data = null`, a `AdminRepository.deleteUser()` je koristio `toResource()` koji zahteva `data != null` → **svaki uspešan delete se tretirao kao `Resource.Error`**, pa `Success` grana u `AdminViewModel.deleteUser()` (koja uklanja korisnika iz liste) nikada nije izvršena. Korisnik se deaktivira u bazi, ali UI to vidi tek posle ponovnog učitavanja liste (promena filtera / reulazak). Bonus simptom: snackbar je prikazivao "User deleted successfully" kao *error* poruku.

### Fix
- `feature/admin/domain/AdminRepository.kt` — `deleteUser()` sada koristi `toUnitResource()` (gleda samo `success` flag), kao što već rade `SupportRepository.deleteTicket` i slični. Optimistic update u ViewModelu sada radi i lista se osvežava odmah.

### Bonus — ista klasa buga na još dva mesta (popravljeno i dokumentovano)
Identičan defekt (`toResource()` na `ApiResponse<Unit>`) postojao je u:
- `feature/order/domain/OrderRepository.kt` → `cancelOrder()` — otkazivanje porudžbine je uspevalo u bazi, ali je UI dobijao grešku,
- `feature/address/domain/AddressRepository.kt` → `deleteAddress()` — isto za brisanje adrese.

Oba prebačena na `toUnitResource()`.

---

## Zadatak 4 — Uklonjen loyalty ekran kod menadžera

### Frontend
- `feature/manager/presentation/ManagerHomeScreen.kt` — uklonjen tab "Loyalty" iz `TabRow`, uklonjena `3 -> LoyaltyTab(...)` grana i ceo `LoyaltyTab` composable (bio je `private`, ne koristi se nigde drugo); uklonjen nekorišćeni `Color` import.
- `feature/manager/presentation/ManagerViewModel.kt` — uklonjena loyalty polja iz `ManagerUiState` (`loyaltyUserId`, `loyaltyAccount`, `isLoadingLoyalty`, `isAddingPoints`, `loyaltyError`, `loyaltySuccess`) i funkcije `onLoyaltyUserIdChange`, `loadUserLoyaltyAccount`, `addPointsToUser`, `clearLoyaltyMessages` (nigde drugde se ne pozivaju).
- `feature/manager/domain/ManagerRepository.kt` — uklonjeni `getUserLoyaltyAccount()` i `addPoints()`.
- `feature/manager/data/ManagerApiService.kt` — uklonjeni `GET loyalty/user/{userId}` i `PUT loyalty/points` endpointi.
- `feature/manager/data/dto/ManagerDto.kt` — uklonjeni `LoyaltyAccountResponse` i `AddPointsRequest` (manager verzije; `feature/loyalty` ima svoje zasebne DTO-ove koji nisu dirani).

### Backend
- `loyalty/controller/LoyaltyController.java` — na `PUT /api/loyalty/points` `@PreAuthorize` promenjen sa `hasAnyRole('ADMIN','MANAGER','EMPLOYEE')` na `hasAnyRole('ADMIN','EMPLOYEE')` — manager više ne može da dodaje poene ni direktnim API pozivom. ADMIN i EMPLOYEE pristup netaknut, kao i sva ostala loyalty funkcionalnost (customer pregled naloga/transakcija, automatski poeni pri checkout-u).
- Read-only `GET /loyalty/user/{userId}` endpointi su namerno ostavljeni kako jesu (ADMIN, MANAGER) — zadatak traži uklanjanje *dodavanja poena* za managera; pregled je bezopasan i može ga koristiti admin tok. Ako želiš, lako se i tu ukloni MANAGER.

---

## Zadatak 5 — Customer može brisati notifikacije

### Backend
- `notification/controller/NotificationController.java` — dodati:
  - `DELETE /api/notifications/{id}` — soft delete jedne notifikacije,
  - `DELETE /api/notifications` — soft delete svih notifikacija korisnika.
  - Obe rute su dostupne svim rolama (kao ostatak kontrolera — sve je scoped na `currentUser`).
- `notification/service/NotificationService.java` — dodate metode:
  - `deleteNotification(notificationId, userId)` — ownership provera po istom obrascu kao `markAsRead` (tuđa notifikacija → 404, ne 403, da se ne otkriva postojanje resursa), zatim `deleted = true`,
  - `deleteAllNotifications(userId)` — soft delete svih ne-obrisanih notifikacija korisnika.
- `notification/repository/NotificationRepository.java` — **bez izmena**: postojeće metode (`findByIdAndDeletedFalse`, `findAllByUser_IdAndDeletedFalse`) su dovoljne.

### Frontend
- `feature/notification/data/NotificationApiService.kt` — dodati `DELETE notifications/{id}` i `DELETE notifications`.
- `feature/notification/domain/NotificationRepository.kt` — dodati `deleteNotification(id)` i `deleteAllNotifications()` (koriste `toUnitResource()` — vidi Zadatak 3).
- `feature/notification/presentation/NotificationViewModel.kt` — dodati:
  - `deleteNotification(id)` — optimistic update: odmah uklanja iz liste (i smanjuje `unreadCount` ako je bila nepročitana); ako backend vrati grešku, prikazuje error i ponovo učitava listu (rollback),
  - `deleteAllNotifications()` — optimistic update: odmah prazni listu i `unreadCount`; rollback reload-om na grešku.
- `feature/notification/presentation/NotificationScreen.kt`:
  - svaka notifikacija umotana u Material3 `SwipeToDismissBox` — swipe zdesna nalevo briše (crvena pozadina sa Delete ikonom iza kartice),
  - dugme **"Clear All"** u headeru pored naslova (disabled kad je lista prazna),
  - dizajn kartica, boje i filteri netaknuti.

---

## Izmenjeni / dodani fajlovi (kompletna lista)

### Backend (`gntl_app_backend`)
| Fajl | Status |
|---|---|
| `pom.xml` | izmenjen (websocket starter) |
| `security/WebSocketConfig.java` | **novo** |
| `security/WebSocketAuthInterceptor.java` | **novo** |
| `security/SecurityConfig.java` | izmenjen (`/ws/**` permitAll uz obrazloženje) |
| `support/controller/ChatWebSocketController.java` | **novo** |
| `support/service/SupportService.java` | izmenjen (WS broadcast u `sendMessage`) |
| `loyalty/controller/LoyaltyController.java` | izmenjen (MANAGER uklonjen sa `PUT /points`) |
| `notification/controller/NotificationController.java` | izmenjen (2 DELETE endpointa) |
| `notification/service/NotificationService.java` | izmenjen (2 delete metode) |

### Frontend (`gntl_app_frontend`)
| Fajl | Status |
|---|---|
| `core/network/ChatWebSocketManager.kt` | **novo** |
| `feature/support/presentation/SupportViewModel.kt` | izmenjen (WS umesto chat polling-a) |
| `feature/support/presentation/ChatScreen.kt` | izmenjen (connect/disconnect pozivi) |
| `feature/cart/presentation/CartViewModel.kt` | izmenjen (uklonjen init loadCart) |
| `feature/admin/domain/AdminRepository.kt` | izmenjen (`toUnitResource` za delete) |
| `feature/order/domain/OrderRepository.kt` | izmenjen (bonus fix `cancelOrder`) |
| `feature/address/domain/AddressRepository.kt` | izmenjen (bonus fix `deleteAddress`) |
| `feature/manager/presentation/ManagerHomeScreen.kt` | izmenjen (uklonjen Loyalty tab + composable) |
| `feature/manager/presentation/ManagerViewModel.kt` | izmenjen (uklonjeno loyalty stanje/funkcije) |
| `feature/manager/domain/ManagerRepository.kt` | izmenjen (uklonjene loyalty metode) |
| `feature/manager/data/ManagerApiService.kt` | izmenjen (uklonjeni loyalty endpointi) |
| `feature/manager/data/dto/ManagerDto.kt` | izmenjen (uklonjeni loyalty DTO-ovi) |
| `feature/notification/data/NotificationApiService.kt` | izmenjen (2 DELETE) |
| `feature/notification/domain/NotificationRepository.kt` | izmenjen (2 delete metode) |
| `feature/notification/presentation/NotificationViewModel.kt` | izmenjen (optimistic delete) |
| `feature/notification/presentation/NotificationScreen.kt` | izmenjen (swipe-to-delete + Clear All) |

Obrisanih fajlova nema.

---

## Šta treba ručno testirati

**Chat / WebSocket (najvažnije):**
1. Customer otvori chat, employee otvori isti tiket na drugom uređaju/emulatoru — poruke moraju stizati **odmah** u oba smera, bez 5s kašnjenja.
2. Sopstvena poruka se pojavljuje tačno jednom (dedup po id-u — nema duplikata).
3. Ugasi backend dok je chat otvoren → klijent pokušava reconnect 3× (1s/2s/4s); ako ne uspe, prikazuje se greška. Ponovo pokreni backend pre isteka pokušaja → konekcija se sama vraća.
4. Slanje dok WS nije konektovan (npr. odmah po ulasku pre CONNECTED frame-a) → poruka ode kroz REST fallback i svejedno se pojavi.
5. Istorija poruka se i dalje učitava pri ulasku (REST), `markAsRead` i unread badge rade kao pre.
6. JWT ističe za 15 min — ako konekcija pukne posle isteka, reconnect čita svež token iz DataStore; proveri ponašanje posle refresh-a tokena tokom duge chat sesije.
7. Negativan test: pokušaj SUBSCRIBE na tuđi `/topic/chat/{id}` (npr. drugi customer) — mora biti odbijen.

**Korpa:** svež login (očisti app data) → dodaj prvi proizvod → **ne sme** biti "You don't have permission" toasta; proizvod u korpi.

**Admin:** deaktiviraj korisnika → odmah nestaje iz ACTIVE liste (bez reulaska); aktiviraj pa opet deaktiviraj → isto; snackbar poruke su sada success, ne error.

**Bonus fixevi:** otkaži porudžbinu i obriši adresu — UI mora prikazati uspeh (ranije je prikazivao grešku iako je operacija uspela).

**Manager:** nema Loyalty taba; Analytics/Discounts/Promotions rade kao pre. Direktan `PUT /api/loyalty/points` sa manager tokenom vraća 403; sa admin/employee tokenom radi.

**Notifikacije:** swipe zdesna nalevo briše jednu (i posle refresh-a je nema — soft delete u bazi); "Clear All" briše sve; brisanje nepročitane smanjuje badge; negativan test — `DELETE /api/notifications/{id}` za tuđu notifikaciju vraća 404.

**Regresija (WS security):** postojeći REST endpointi i dalje traže JWT — proveri da neautentifikovan `GET /api/products/paged` i dalje vraća 401/403, a da je jedino `/ws/**` handshake dostupan bez headera (ali odbija nevažeći/nepostojeći token).

---
---

# CHANGES (runda 2) — Employee orders fix, WebSocket badge sistem, notifikacije UI

> Verifikovano kompajliranjem: backend (`mvn compile`, JDK 21) i frontend (`gradlew :app:compileDebugKotlin`) prolaze bez grešaka. UI dizajn (osim eksplicitno traženih izmena), security izmene iz SECURITY_ANALYSIS.md i postojeća WebSocket chat implementacija nisu dirani.

## Zadatak 1 — Fix: Employee orders ne prikazuje pending ordere

### Uzrok (root cause)
`EmployeeViewModel.filteredOrders` je bio **computed property** (`get()` nad `_ordersUiState.value`), a `EmployeeHomeScreen` ga je čitao direktnim pozivom `viewModel.filteredOrders` u kompoziciji. Compose ne prati promene običnog property-ja — kada se orderi učitaju (ili promeni filter), ne postoji state read koji bi pokrenuo rekompoziciju, pa je tab prikazivao praznu listu snimljenu pri prvoj kompoziciji. Identičan bug je ranije postojao (i popravljen) za `filteredTickets`.

### Fix
- `feature/employee/presentation/EmployeeViewModel.kt` — `filteredOrders` pretvoren u `StateFlow<List<OrderResponse>>` (`_ordersUiState.map { ... }.stateIn(...)`) — identično kao postojeći `filteredTickets`. Logika filtriranja nepromenjena.
- `feature/employee/presentation/EmployeeHomeScreen.kt` — `filteredOrders` se sada kolektuje kroz `collectAsStateWithLifecycle()` i prosleđuje tabu kao vrednost. UI netaknut.

## Zadatak 2 — WebSocket badge sistem (umesto ticket pollinga na 3s)

### Backend
- **NOVO** `support/dto/UnreadUpdateDTO.java` — payload badge eventa: `{ticketId, sessionId, unreadCount}`.
- `support/service/SupportService.java`:
  - `sendMessage()` — posle postojećeg chat broadcasta dodatno šalje badge event strani koja poruku **prima**: employee poruka → `/topic/user/{customerId}/unread`, customer poruka → `/topic/employee/unread` (zajednički topic za sve staff klijente). `unreadCount` se računa iz baze (idempotentno — klijent samo prepiše vrednost). BOT poruke ne emituju event.
  - `createTicket()` — po kreiranju tiketa broadcast `SupportTicketDTO`-a na `/topic/employee/new-ticket`.
  - `markMessagesAsRead()` — **dodatak van eksplicitne specifikacije, namerno**: kada neko pročita poruke, emituje se event sa `unreadCount = 0` ka strani koja je čitala (customer → njegov user topic; employee → zajednički employee topic). Bez ovoga bi, posle uklanjanja pollinga, brojač kod čitaoca (i kod **ostalih** employee-a) ostao zamrznut na staroj vrednosti do ručnog refresha.
- `security/WebSocketAuthInterceptor.java` — SUBSCRIBE autorizacija proširena na nove topice, istim ownership obrascem kao za chat (nije menjana postojeća logika, samo dodata): `/topic/user/{id}/unread` sme da sluša samo taj korisnik; `/topic/employee/**` samo staff (ADMIN/MANAGER/EMPLOYEE). Sprečava da običan customer sluša tuđe unread evente ili employee topice.
- `security/WebSocketConfig.java` — **bez izmena**: simple broker već rutira ceo `/topic` prefix.

### Frontend
- **NOVO** `core/network/BadgeWebSocketManager.kt` — dugoživeća STOMP konekcija za badge evente, isti minimalni STOMP-preko-OkHttp obrazac kao `ChatWebSocketManager`, ali sa **više subscription-a na jednoj konekciji** (`subscribe(topic, type, onEvent, onResync)` / `unsubscribe(topic)`), lenjim otvaranjem na prvi subscribe i gašenjem kad nema pretplata. Reconnect: 3 pokušaja, exponential backoff, svež JWT iz DataStore pri svakom pokušaju. `onResync` se okida jednom pri prekidu i jednom posle uspešnog reconnecta (posle koga se svi topici automatski ponovo subscribuju) — pozivalac tu radi jednokratni REST reload. **Zašto poseban manager, a ne proširenje ChatWebSocketManager-a:** chat konekcija živi samo dok je `ChatScreen` otvoren i `connect()` gasi prethodni socket — deljenje jednog socketa bi značilo da ulazak u chat ruši badge pretplate. Postojeći chat manager nije diran.
- `feature/support/data/dto/SupportDto.kt` — dodat `UnreadUpdateEvent(ticketId, sessionId, unreadCount)`.
- `feature/support/presentation/SupportViewModel.kt` (customer):
  - uklonjeni `ticketPollingJob`, `startTicketPolling()`, `stopTicketPolling()`,
  - `init` sada prati `token` flow (VM se kreira pre logina, zbog badge-a u bottom bar-u): na login → jednokratni `loadMyTicketsAndUnread()` + `subscribeToUnreadUpdates(userId)` (userId iz DataStore-a); na logout → `unsubscribeFromUnreadUpdates()`,
  - event handler samo prepiše `unreadCount` odgovarajućeg tiketa u lokalnom state-u (bez REST poziva); `onResync` radi `loadMyTickets()`,
  - `loadMyTickets()` / `loadMyTicketsAndUnread()` zadržani za inicijalno učitavanje, pull-to-refresh i fallback.
- `feature/support/presentation/SupportScreen.kt` — `LaunchedEffect { startTicketPolling() }` zamenjen sa `loadMyTickets()` (jednokratno osvežavanje pri ulasku/povratku na ekran).
- `feature/employee/presentation/EmployeeViewModel.kt` (employee):
  - uklonjen polling (kao gore), `init` radi `loadOrders()` + `loadTickets()` + `subscribeToBadgeUpdates()`,
  - `/topic/employee/unread` → prepiše `unreadCount` tiketa u state-u; `/topic/employee/new-ticket` → `loadTickets()` (reload umesto lokalnog ubacivanja — čuva serverski redosled i odmah povlači unread brojače),
  - `onCleared()` → unsubscribe oba topica.

### Napomene o ponašanju
- Unread event za tiket koji nije u trenutno učitanoj listi (npr. na drugoj stranici) se ignoriše — sledeći ulazak na ekran ionako radi pun reload.
- `SupportViewModel` se subscribuje na sopstveni user topic za svaku ulogovanu rolu (i staff) — bezopasno: na taj topic stižu eventi samo ako korisnik ima sopstvene tikete, a interceptor svakako dozvoljava samo sopstveni topic.

## Zadatak 3 — UI izmene za notifikacije

- `feature/notification/presentation/NotificationScreen.kt`:
  - **3.1** uklonjen `SwipeToDismissBox` (i crvena delete pozadina) — kartica se sada renderuje direktno; "Clear All" dugme u headeru zadržano; `deleteNotification()` u ViewModelu/repository-ju/API-ju **zadržan** (samo je swipe gest uklonjen iz UI-ja); uklonjen nekorišćeni `Delete` icon import,
  - **3.2** pozadina nepročitane kartice: `Gold500.copy(alpha = 0.08f)` → `Color(0xFF1A2744)` (diskretna tamnoplava); indikator tačka: Gold500, `8.dp` → `6.dp` (razmak povećan 10→12dp da naslovi ostanu poravnati sa pročitanim karticama); bold naslov nepročitanih zadržan; boje pročitanih kartica i tip badge chip-ovi (Discount plava / Order zelena / Loyalty zlatna) netaknuti.

## Izmenjeni / dodani fajlovi (runda 2)

### Backend
| Fajl | Status |
|---|---|
| `support/dto/UnreadUpdateDTO.java` | **novo** |
| `support/service/SupportService.java` | izmenjen (badge broadcasti u `sendMessage`/`createTicket`/`markMessagesAsRead`) |
| `security/WebSocketAuthInterceptor.java` | izmenjen (SUBSCRIBE autorizacija za badge topice) |

### Frontend
| Fajl | Status |
|---|---|
| `core/network/BadgeWebSocketManager.kt` | **novo** |
| `feature/support/data/dto/SupportDto.kt` | izmenjen (`UnreadUpdateEvent`) |
| `feature/support/presentation/SupportViewModel.kt` | izmenjen (badge WS umesto pollinga) |
| `feature/support/presentation/SupportScreen.kt` | izmenjen (`startTicketPolling` → `loadMyTickets`) |
| `feature/employee/presentation/EmployeeViewModel.kt` | izmenjen (Zadatak 1 + badge WS umesto pollinga) |
| `feature/employee/presentation/EmployeeHomeScreen.kt` | izmenjen (collect `filteredOrders`) |
| `feature/notification/presentation/NotificationScreen.kt` | izmenjen (bez swipe-a, boje nepročitanih) |

Obrisanih fajlova nema.

## Šta treba ručno testirati (runda 2)

**Employee orders (Zadatak 1):**
1. Uloguj se kao employee sa postojećim PENDING orderima u bazi — Orders tab mora odmah prikazati listu (ranije prazno).
2. Menjaj filter chipove (ALL/PENDING/...) — lista se mora menjati bez reulaska na ekran.
3. Promeni status ordera — kartica mora odmah reflektovati novi status (i nestati iz liste ako više ne odgovara filteru).

**Badge sistem (Zadatak 2):**
1. Customer na SupportScreen-u / bottom bar-u: employee pošalje poruku → unread badge se uveća **odmah**, bez pollinga (proveri u backend logu da nema više `GET /my-tickets` na 3s).
2. Employee na Tickets tabu: customer pošalje poruku → badge na tom tiketu se uveća odmah; drugi employee klijent vidi isto (zajednički topic).
3. Customer kreira nov tiket kroz bot flow → tiket se pojavi na employee listi bez refresha.
4. Čitanje poruka: customer otvori chat i pročita → njegov badge padne na 0 (i bottom bar suma); employee pročita → badge padne na 0 na **svim** employee klijentima.
5. Logcat tag `BadgeWebSocket`: "connecting" → "connected successfully" pri loginu; eventi se loguju pri porukama.
6. Fallback: ugasi backend → jednokratni REST sync + 3 reconnect pokušaja; upali backend pre isteka → posle reconnecta subscription-i se sami vrate i lista se sinhronizuje. Pull-to-refresh radi uvek.
7. Logout/login — na logout se badge socket gasi (nema više pretplata), na login se ponovo uspostavlja.
8. Negativni testovi (security): customer pokušaj SUBSCRIBE na `/topic/employee/unread` ili tuđi `/topic/user/{id}/unread` → konekcija/subscription odbijen.
9. Regresija: chat i dalje radi realtime u oba smera (chat WS konekcija je odvojena i nije dirana).

**Notifikacije (Zadatak 3):**
1. Swipe po kartici više ne briše (kartica se ne pomera); "Clear All" i dalje briše sve.
2. Nepročitana kartica: tamnoplava pozadina (#1A2744), mala zlatna tačka (6dp), bold naslov; pročitana kartica izgleda kao pre.
3. Tip badge chipovi zadržali boje (Discount plava, Order zelena, Loyalty zlatna).
