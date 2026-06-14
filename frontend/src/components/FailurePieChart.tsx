import { useRef } from 'react';
import { motion } from 'framer-motion';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Text } from '@react-three/drei';
import * as THREE from 'three';
import type { DashboardStats, FailureType } from '../types';
import { FAILURE_COLORS, FAILURE_LABELS } from '../types';

interface Props {
  stats: DashboardStats;
}

interface PieEntry {
  type: FailureType;
  name: string;
  value: number;
  color: string;
}

function PieSegment({ value, total, color, startAngle, radius = 2.5 }: any) {
  const meshRef = useRef<THREE.Mesh>(null);
  
  // Angle for this segment
  const angle = (value / total) * Math.PI * 2;
  
  // Midpoint angle for label placement
  const midAngle = startAngle + angle / 2;
  const labelRadius = radius + 0.6;
  const lx = Math.cos(midAngle) * labelRadius;
  const lz = -Math.sin(midAngle) * labelRadius; // Negative Z because Three.js coordinate system

  // Convert hex color to THREE.Color and boost it for neon glow
  const baseColor = new THREE.Color(color);
  const emissiveColor = baseColor.clone().multiplyScalar(0.5);

  return (
    <group>
      {/* 3D Cylinder Segment */}
      <mesh ref={meshRef} position={[0, 0, 0]} rotation={[-Math.PI / 2, 0, startAngle]}>
        {/* args: radiusTop, radiusBottom, height, radialSegments, heightSegments, openEnded, thetaStart, thetaLength */}
        <cylinderGeometry args={[radius, radius, 0.8, 32, 1, false, 0, angle]} />
        <meshStandardMaterial 
          color={baseColor} 
          emissive={emissiveColor}
          emissiveIntensity={0.6}
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>
      
      {/* 3D Label */}
      {value / total > 0.05 && (
        <Text 
          position={[lx, 0.5, lz]} 
          rotation={[-Math.PI / 4, 0, 0]}
          fontSize={0.3} 
          color="#ffffff"
          font="https://fonts.gstatic.com/s/jetbrainsmono/v18/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKwI.woff"
          anchorX="center"
          anchorY="middle"
        >
          {`${((value / total) * 100).toFixed(0)}%`}
        </Text>
      )}
    </group>
  );
}

function RotatingGroup({ children }: { children: React.ReactNode }) {
  const groupRef = useRef<THREE.Group>(null);

  useFrame((_, delta) => {
    if (groupRef.current) {
      groupRef.current.rotation.y -= delta * 0.2; // Slow auto-rotation
    }
  });

  return (
    <group ref={groupRef} rotation={[0.4, 0, 0]}>
      {children}
    </group>
  );
}

export default function FailurePieChart({ stats }: Props) {
  const pieData: PieEntry[] = (
    [
      ['infra',    stats.infraFailures],
      ['test',     stats.testFailures],
      ['build',    stats.buildFailures],
      ['security', stats.securityFailures],
      ['unknown',  stats.unknownFailures],
    ] as [FailureType, number][]
  )
    .filter(([, v]) => v > 0)
    .map(([type, value]) => ({
      type,
      name:  FAILURE_LABELS[type],
      value,
      color: FAILURE_COLORS[type],
    }));

  const total = pieData.reduce((s, d) => s + d.value, 0);
  let currentAngle = 0;

  return (
    <motion.div
      className="card"
      style={{ display: 'flex', flexDirection: 'column' }}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.3 }}
    >
      <div className="card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--sp-3)' }}>
          <div className="section-title-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a10 10 0 0 1 10 10"/></svg>
          </div>
          <span className="card-title" style={{ fontSize: '0.9rem', textTransform: 'none', fontWeight: 700, color: 'var(--text-primary)' }}>
            Breakdown (Today)
          </span>
        </div>
        <div style={{ fontFamily: 'var(--font-mono)', fontWeight: 800, fontSize: '1.1rem', color: 'var(--accent)' }}>
          {total}
        </div>
      </div>

      {total === 0 ? (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 8, color: 'var(--text-muted)', height: 280 }}>
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" opacity="0.4"><path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"/><path d="M8 15h.01M16 15h.01M9 9s.5-2 3-2 3 2 3 2"/></svg>
          <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>No failures today 🎉</span>
        </div>
      ) : (
        <div style={{ position: 'relative', width: '100%', height: 280 }}>
          <Canvas camera={{ position: [0, 6, 8], fov: 45 }}>
            <ambientLight intensity={0.5} />
            <pointLight position={[10, 15, 10]} intensity={1.5} />
            <spotLight position={[-10, 10, -10]} intensity={0.5} angle={0.3} penumbra={1} />
            
            <RotatingGroup>
              {pieData.map((d) => {
                const segment = (
                  <PieSegment
                    key={d.type}
                    value={d.value}
                    total={total}
                    color={d.color}
                    startAngle={currentAngle}
                  />
                );
                currentAngle += (d.value / total) * Math.PI * 2;
                return segment;
              })}
            </RotatingGroup>
            
            <OrbitControls 
              enableZoom={false} 
              enablePan={false}
              maxPolarAngle={Math.PI / 2} // Prevent looking from below
              minPolarAngle={Math.PI / 6}
            />
          </Canvas>

          {/* Custom 2D Legend Overlay */}
          <div style={{
            position: 'absolute',
            bottom: 12,
            left: 0,
            right: 0,
            display: 'flex',
            justifyContent: 'center',
            gap: 16,
            flexWrap: 'wrap',
            pointerEvents: 'none'
          }}>
            {pieData.map(d => (
              <div key={d.type} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ width: 10, height: 10, borderRadius: '50%', background: d.color, display: 'inline-block', boxShadow: `0 0 6px ${d.color}` }} />
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>{d.name}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </motion.div>
  );
}
