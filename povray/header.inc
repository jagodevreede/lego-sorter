
#declare LDXQual = 3;
#declare LDXSW = 0; // Seam Width (0 for no seams)
#declare LDXFloor = 0; // Include Floor? (1 = YES; 0 = NO)
#declare LDXCameraLoc = <CAMLOC>; // Camera Location vector
#declare LDXCameraLookAt = < 0,0,0 >; // Camera look-at point vector
#declare LDXCameraSky = < 0,-1, 0 >; // Camera sky vector (<0,-1,0> will usually work for standard "up")
#declare LDXCameraAngle = 0;
#declare LDXBackground = 1;
#declare LDXBgR = <LDXBgR>; 	// Background Red
#declare LDXBgG = <LDXBgG>;	// Background Green
#declare LDXBgB = <LDXBgB>;	// Background Blue
// Disable default lights
#declare LDXSkipLight1 = 1;
#declare LDXSkipLight2 = 1;
#declare LDXSkipLight3 = 1;
#ifndef (LDXCenterX) #declare LDXCenterX = 0; #end
#ifndef (LDXCenterY) #declare LDXCenterY = -17; #end
#ifndef (LDXCenterZ) #declare LDXCenterZ = 0; #end
#ifndef (LDXCenter) #declare LDXCenter = <LDXCenterX,LDXCenterY,LDXCenterZ>; #end
//#ifndef (LDXRadius) #declare LDXRadius = 38.678158; #end
#ifndef (LDXRadius) #declare LDXRadius = 348.678158; #end

// Lights
light_source {	// Latitude,Longitude: 45,0,LDXRadius*2
//	<0*LDXRadius,-1.414214*LDXRadius,-1.414214*LDXRadius> + LDXCenter
    <40,0,0> + LDXCenter
	color rgb <LIGHT1RGB>
	jitter
}
light_source {	// Latitude,Longitude: 45,0,LDXRadius*2
    <0,40,0> + LDXCenter
	color rgb <LIGHT1RGB>
	jitter
}
light_source {	// Latitude,Longitude: 45,0,LDXRadius*2
    <0,0,40> + LDXCenter
	color rgb <LIGHT1RGB>
	jitter
}
//light_source {	// Latitude,Longitude: 30,120,LDXRadius*2
//	<1.5*LDXRadius,-1*LDXRadius,0.866026*LDXRadius> + LDXCenter
//	color rgb <LIGHT2RGB>
//	jitter
//}
//light_source {	// Latitude,Longitude: 60,-120,LDXRadius*2
//	<-0.866025*LDXRadius,-1.732051*LDXRadius,0.5*LDXRadius> + LDXCenter
//	color rgb <LIGHT3RGB>
//	jitter
//}