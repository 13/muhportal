import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:muhportal/main.dart';

void main() {
  testWidgets('MuhPortal app smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const MuhPortalApp());

    // Wait for the app to load
    await tester.pumpAndSettle();

    // Verify that the app renders without crashing
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
