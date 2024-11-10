void main() {
  var someValue = "Hello";
  switch(someValue) {
    case "Hello":
      print("Bello");
    case "Bye":
      print('Mellow!');
  }
  someValue = "Bye";
  switch(someValue) {
    case 'Bye':
      print("Why?");
  }
}
