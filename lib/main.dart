import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const App());

const _ch = MethodChannel('com.mydev.notifpop/ch');

class App extends StatelessWidget {
  const App({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Notif Pop',
    theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
    home: const Home(),
    debugShowCheckedModeBanner: false,
  );
}

class AppInfo { final String name, pkg; AppInfo(this.name, this.pkg); }

class Home extends StatefulWidget {
  const Home({super.key});
  @override State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  List<AppInfo> apps = [];
  Set<String> allowed = {};
  int r=30, g=30, b=46, a=204;
  bool hasOverlay=false, hasNL=false, loading=true;
  String search = '';

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final d = jsonDecode(await _ch.invokeMethod('load') as String) as Map;
      setState(() {
        apps = (d['apps'] as List).map((x) => AppInfo(x['n'], x['p'])).toList();
        allowed = Set<String>.from(d['allowed'] ?? []);
        r=d['r']??30; g=d['g']??30; b=d['b']??46; a=d['a']??204;
        hasOverlay=d['hasOverlay']??false; hasNL=d['hasNL']??false;
        loading = false;
      });
    } catch (e) { setState(() => loading=false); }
  }

  Future<void> _save() => _ch.invokeMethod('save', {'allowed': allowed.toList(), 'r': r, 'g': g, 'b': b, 'a': a});

  Color get _color => Color.fromARGB(a, r, g, b);
  List<AppInfo> get _filtered => search.isEmpty ? apps : apps.where((x) => x.name.toLowerCase().contains(search.toLowerCase())).toList();

  Widget _slider(String l, int v, Color c, Function(int) f) => Row(children: [
    SizedBox(width: 20, child: Text(l, style: TextStyle(color: c, fontWeight: FontWeight.bold, fontSize: 13))),
    Expanded(child: Slider(value: v.toDouble(), min: 0, max: 255, activeColor: c, onChanged: (x) { setState(() => f(x.toInt())); _save(); })),
    SizedBox(width: 32, child: Text('$v', style: const TextStyle(fontSize: 12))),
  ]);

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: const Color(0xFF0F0F1A),
    appBar: AppBar(
      backgroundColor: const Color(0xFF1A1A2E),
      title: const Text('Notif Pop', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      actions: [IconButton(icon: const Icon(Icons.refresh, color: Colors.white), onPressed: _load)],
    ),
    body: loading ? const Center(child: CircularProgressIndicator())
        : RefreshIndicator(onRefresh: _load, child: ListView(padding: const EdgeInsets.all(14), children: [
      if (!hasOverlay || !hasNL) Card(color: Colors.red.shade900, child: Padding(padding: const EdgeInsets.all(12), child: Column(children: [
        if (!hasOverlay) ListTile(leading: const Icon(Icons.warning, color: Colors.orange), title: const Text('Overlay Permission', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)), trailing: TextButton(onPressed: () => _ch.invokeMethod('requestOverlay'), child: const Text('Grant', style: TextStyle(color: Colors.orange)))),
        if (!hasNL) ListTile(leading: const Icon(Icons.notifications_off, color: Colors.orange), title: const Text('Notification Access', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)), trailing: TextButton(onPressed: () => _ch.invokeMethod('requestNL'), child: const Text('Grant', style: TextStyle(color: Colors.orange)))),
      ]))),
      const SizedBox(height: 10),
      Card(color: const Color(0xFF1E1E2E), child: Padding(padding: const EdgeInsets.all(14), child: Row(children: [
        Container(width: 12, height: 12, decoration: BoxDecoration(color: hasOverlay&&hasNL ? Colors.green : Colors.orange, shape: BoxShape.circle)),
        const SizedBox(width: 10),
        Expanded(child: Text(hasOverlay&&hasNL ? 'Active - Popups enabled' : 'Inactive - Grant permissions', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600))),
        if (hasOverlay&&hasNL) OutlinedButton(onPressed: () => _ch.invokeMethod('testPopup'), child: const Text('Test')),
      ]))),
      const SizedBox(height: 10),
      Card(color: const Color(0xFF1E1E2E), child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Text('Popup Color', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
        const SizedBox(height: 10),
        _slider('R', r, Colors.red, (v) => r=v),
        _slider('G', g, Colors.green, (v) => g=v),
        _slider('B', b, Colors.blue, (v) => b=v),
        _slider('A', a, Colors.grey, (v) => a=v),
        const SizedBox(height: 10),
        Container(padding: const EdgeInsets.all(14), decoration: BoxDecoration(color: _color, borderRadius: BorderRadius.circular(14), border: Border.all(color: Colors.white24)), child: const Row(children: [
          Icon(Icons.android, color: Colors.white, size: 38),
          SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('App Name', style: TextStyle(color: Colors.white70, fontSize: 11)),
            Text('Notification Title', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
            Text('This is the message preview', style: TextStyle(color: Colors.white, fontSize: 12)),
          ])),
        ])),
      ]))),
      const SizedBox(height: 10),
      Card(color: const Color(0xFF1E1E2E), child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          const Text('Apps', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
          const Spacer(),
          TextButton(onPressed: () { setState(() => allowed=apps.map((x)=>x.pkg).toSet()); _save(); }, child: const Text('All')),
          TextButton(onPressed: () { setState(() => allowed.clear()); _save(); }, child: const Text('None')),
        ]),
        Text(allowed.isEmpty ? 'All apps' : '${allowed.length} selected', style: const TextStyle(color: Colors.grey, fontSize: 12)),
        const SizedBox(height: 8),
        TextField(
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(hintText: 'Search...', hintStyle: const TextStyle(color: Colors.grey), prefixIcon: const Icon(Icons.search, color: Colors.grey), border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)), filled: true, fillColor: const Color(0xFF0F0F1A)),
          onChanged: (v) => setState(() => search=v),
        ),
        const SizedBox(height: 8),
        ConstrainedBox(constraints: const BoxConstraints(maxHeight: 400), child: ListView.builder(
          shrinkWrap: true,
          itemCount: _filtered.length,
          itemBuilder: (_, i) {
            final app = _filtered[i];
            return CheckboxListTile(dense: true, title: Text(app.name, style: const TextStyle(color: Colors.white, fontSize: 13)), subtitle: Text(app.pkg, style: const TextStyle(color: Colors.grey, fontSize: 10)), value: allowed.contains(app.pkg), activeColor: Colors.indigo, onChanged: (v) { setState(() { if (v==true) allowed.add(app.pkg); else allowed.remove(app.pkg); }); _save(); });
          },
        )),
      ]))),
    ])),
  );
}
